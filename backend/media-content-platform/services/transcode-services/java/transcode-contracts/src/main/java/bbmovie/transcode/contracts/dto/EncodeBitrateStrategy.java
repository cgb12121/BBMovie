package bbmovie.transcode.contracts.dto;

/**
 * How CAS / VES applies {@link EncodeRequest} bitrate hint fields for libx264 rate control.
 */
public enum EncodeBitrateStrategy {
    DEFAULT,
    VBV_ABR,
    VBV_CRF_CAP
}
