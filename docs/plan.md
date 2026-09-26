# Implementation and verification status

Duplicard 1.1.2 is implemented as an offline Android loyalty-card wallet. It supports EAN-13, EAN-8, UPC-A, UPC-E, Code 128, Code 39, ITF and QR; camera/manual entry with three-read stabilization; automatic or explicit format selection; preset/custom card colors; add/edit/delete/reorder; bright checkout display; atomic storage; and versioned JSON import/export.

Version-1 EAN-13 data migrates on read with the default green color. Version-2 backups preserve format and color. Duplicate identity uses both barcode format and content.

Fresh verification: 12 Android tests and 48 standalone core checks pass; optimized release assembly and lint pass; APK v2/v3 signatures verify; no Internet permission is present. Remaining work is the physical-phone checklist in `VERIFICATION.md`.
