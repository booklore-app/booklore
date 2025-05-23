package com.adityachandel.booklore.model.dto;

import com.adityachandel.booklore.model.dto.settings.SidebarSortOption;
import com.adityachandel.booklore.model.enums.CbxPageSpread;
import com.adityachandel.booklore.model.enums.CbxPageViewMode;
import com.adityachandel.booklore.model.enums.ProvisioningMethod;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
public class BookLoreUser {
    private Long id;
    private String username;
    private boolean isDefaultPassword;
    private String name;
    private String email;
    private ProvisioningMethod provisioningMethod;
    private List<Library> assignedLibraries;
    private UserPermissions permissions;
    private UserSettings userSettings;

    @Data
    public static class UserPermissions {
        private boolean isAdmin;
        private boolean canUpload;
        private boolean canDownload;
        private boolean canEditMetadata;
        private boolean canManipulateLibrary;
        private boolean canEmailBook;
    }

    @Data
    public static class UserSettings {
        public PerBookSetting perBookSetting;
        public PdfReaderSetting pdfReaderSetting;
        public EpubReaderSetting epubReaderSetting;
        public CbxReaderSetting cbxReaderSetting;
        public SidebarSortOption sidebarLibrarySorting;
        public SidebarSortOption sidebarShelfSorting;

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class EpubReaderSetting {
            private String theme;
            private String font;
            private int fontSize;
            private String flow;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PdfReaderSetting {
            private String pageSpread;
            private String pageZoom;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class CbxReaderSetting {
            private CbxPageSpread pageSpread;
            private CbxPageViewMode pageViewMode;
        }

        @Data
        @Builder
        @AllArgsConstructor
        @NoArgsConstructor
        public static class PerBookSetting {
            private GlobalOrIndividual pdf;
            private GlobalOrIndividual epub;
            private GlobalOrIndividual cbx;

            public enum GlobalOrIndividual {
                Global, Individual
            }
        }
    }
}
