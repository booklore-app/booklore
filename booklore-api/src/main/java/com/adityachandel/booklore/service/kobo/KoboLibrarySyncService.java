package com.adityachandel.booklore.service.kobo;

import com.adityachandel.booklore.model.dto.kobo.KoboHeaders;
import com.adityachandel.booklore.model.dto.BookLoreUser;
import com.adityachandel.booklore.model.dto.BookloreSyncToken;
import com.adityachandel.booklore.model.dto.kobo.*;
import com.adityachandel.booklore.model.entity.KoboSnapshotBookEntity;
import com.adityachandel.booklore.model.entity.KoboLibrarySnapshotEntity;
import com.adityachandel.booklore.repository.KoboDeletedBookProgressRepository;
import com.adityachandel.booklore.service.KoboEntitlementService;
import com.adityachandel.booklore.service.KoboLibrarySnapshotService;
import com.adityachandel.booklore.util.RequestUtils;
import com.adityachandel.booklore.util.kobo.BookloreSyncTokenGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpServletRequest;

import java.util.*;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service
@Slf4j
public class KoboLibrarySyncService {

    private final BookloreSyncTokenGenerator tokenGenerator;
    private final KoboLibrarySnapshotService koboLibrarySnapshotService;
    private final KoboEntitlementService entitlementService;
    private final KoboDeletedBookProgressRepository koboDeletedBookProgressRepository;
    private final KoboServerProxy koboServerProxy;
    private final ObjectMapper objectMapper;

    public ResponseEntity<?> syncLibrary(BookLoreUser user, String token) {
        HttpServletRequest request = RequestUtils.getCurrentRequest();
        BookloreSyncToken syncToken = Optional.ofNullable(tokenGenerator.fromRequestHeaders(request)).orElse(new BookloreSyncToken());

        KoboLibrarySnapshotEntity currSnapshot = koboLibrarySnapshotService.findByIdAndUserId(syncToken.getOngoingSyncPointId(), user.getId()).orElseGet(() -> koboLibrarySnapshotService.create(user.getId()));
        Optional<KoboLibrarySnapshotEntity> prevSnapshot = koboLibrarySnapshotService.findByIdAndUserId(syncToken.getLastSuccessfulSyncPointId(), user.getId());

        List<Entitlement> entitlements = new ArrayList<>();
        boolean shouldContinueSync = false;

        if (prevSnapshot.isPresent()) {
            int maxRemaining = 5;
            List<KoboSnapshotBookEntity> addedAll = new ArrayList<>();
            List<KoboSnapshotBookEntity> removedAll = new ArrayList<>();

            koboLibrarySnapshotService.updateSyncedStatusForExistingBooks(prevSnapshot.get().getId(), currSnapshot.getId());

            Page<KoboSnapshotBookEntity> addedPage = koboLibrarySnapshotService.getNewlyAddedBooks(prevSnapshot.get().getId(), currSnapshot.getId(), PageRequest.of(0, maxRemaining), user.getId());
            addedAll.addAll(addedPage.getContent());
            maxRemaining -= addedPage.getNumberOfElements();
            shouldContinueSync = addedPage.hasNext();

            Page<KoboSnapshotBookEntity> removedPage = Page.empty();
            if (addedPage.isLast() && maxRemaining > 0) {
                removedPage = koboLibrarySnapshotService.getRemovedBooks(prevSnapshot.get().getId(), currSnapshot.getId(), user.getId(), PageRequest.of(0, maxRemaining));
                removedAll.addAll(removedPage.getContent());
                shouldContinueSync = shouldContinueSync || removedPage.hasNext();
            }

            Set<Long> addedIds = addedAll.stream().map(KoboSnapshotBookEntity::getBookId).collect(Collectors.toSet());
            Set<Long> removedIds = removedAll.stream().map(KoboSnapshotBookEntity::getBookId).collect(Collectors.toSet());

            entitlements.addAll(entitlementService.generateNewEntitlements(addedIds, token, false));
            entitlements.addAll(entitlementService.generateChangedEntitlements(removedIds, token, true));
        } else {
            int maxRemaining = 5;
            List<KoboSnapshotBookEntity> all = new ArrayList<>();
            while (maxRemaining > 0) {
                var page = koboLibrarySnapshotService.getUnsyncedBooks(currSnapshot.getId(), PageRequest.of(0, maxRemaining));
                all.addAll(page.getContent());
                maxRemaining -= page.getNumberOfElements();
                shouldContinueSync = page.hasNext();
                if (!shouldContinueSync || page.getNumberOfElements() == 0) break;
            }
            Set<Long> ids = all.stream().map(KoboSnapshotBookEntity::getBookId).collect(Collectors.toSet());
            entitlements.addAll(entitlementService.generateNewEntitlements(ids, token, false));
        }

        if (!shouldContinueSync) {
            ResponseEntity<JsonNode> koboStoreResponse = koboServerProxy.proxyCurrentRequest(null, true);
            Collection<Entitlement> syncResultsKobo = Optional.ofNullable(koboStoreResponse.getBody())
                    .map(body -> {
                        try {
                            List<Entitlement> results = new ArrayList<>();
                            if (body.isArray()) {
                                for (JsonNode node : body) {
                                    if (node.has("NewEntitlement")) {
                                        results.add(objectMapper.treeToValue(node, NewEntitlement.class));
                                    } else if (node.has("ChangedEntitlement")) {
                                        results.add(objectMapper.treeToValue(node, ChangedEntitlement.class));
                                    } else {
                                        log.warn("Unknown entitlement type in Kobo response: {}", node);
                                    }
                                }
                            }
                            return results;
                        } catch (Exception e) {
                            log.error("Failed to map Kobo response to Entitlement objects", e);
                            return Collections.<Entitlement>emptyList();
                        }
                    })
                    .orElse(Collections.emptyList());

            entitlements.addAll(syncResultsKobo);

            shouldContinueSync = "continue".equalsIgnoreCase(
                    Optional.ofNullable(koboStoreResponse.getHeaders().getFirst(KoboHeaders.X_KOBO_SYNC)).orElse("")
            );

            String koboSyncTokenHeader = koboStoreResponse.getHeaders().getFirst(KoboHeaders.X_KOBO_SYNCTOKEN);
            syncToken = koboSyncTokenHeader != null ? tokenGenerator.fromBase64(koboSyncTokenHeader) : syncToken;
        }

        if (shouldContinueSync) {
            syncToken.setOngoingSyncPointId(currSnapshot.getId());
        } else {
            prevSnapshot.ifPresent(sp -> koboLibrarySnapshotService.deleteById(sp.getId()));
            koboDeletedBookProgressRepository.deleteBySnapshotIdAndUserId(syncToken.getOngoingSyncPointId(), user.getId());
            syncToken.setOngoingSyncPointId(null);
            syncToken.setLastSuccessfulSyncPointId(currSnapshot.getId());
        }

        return ResponseEntity.ok()
                .header(KoboHeaders.X_KOBO_SYNC, shouldContinueSync ? "continue" : "")
                .header(KoboHeaders.X_KOBO_SYNCTOKEN, tokenGenerator.toBase64(syncToken))
                .body(entitlements);
    }
}