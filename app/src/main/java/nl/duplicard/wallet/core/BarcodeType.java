package nl.duplicard.wallet.core;

public enum BarcodeType {
 EAN_13("EAN-13"),
 EAN_8("EAN-8"),
 UPC_A("UPC-A"),
 UPC_E("UPC-E"),
 CODE_128("Code 128"),
 CODE_39("Code 39"),
 ITF("ITF"),
 QR_CODE("QR code");

 private final String displayName;
 BarcodeType(String displayName) { this.displayName=displayName; }
 public String getDisplayName() { return displayName; }
}
