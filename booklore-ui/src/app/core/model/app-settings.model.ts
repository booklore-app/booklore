import {MetadataRefreshOptions} from '../../metadata/model/request/metadata-refresh-options.model';

export interface CoverSettings {
  resolution: string;
}

export interface AppSettings {
  metadataRefreshOptions: MetadataRefreshOptions;
  coverSettings?: CoverSettings
}
