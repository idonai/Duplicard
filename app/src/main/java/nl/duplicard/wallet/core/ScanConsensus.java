package nl.duplicard.wallet.core;

/** Requires repeated identical camera reads before a scan is accepted. */
public final class ScanConsensus {
 private final int required;
 private BarcodeType format;
 private String value;
 private int count;

 public ScanConsensus(int required) {
  if(required<2) throw new IllegalArgumentException("At least two matching scans are required.");
  this.required=required;
 }
 public int observe(BarcodeType nextFormat,String nextValue) {
  if(nextFormat==format&&java.util.Objects.equals(nextValue,value)) count++;
  else { format=nextFormat;value=nextValue;count=1; }
  return Math.min(count,required);
 }
 public void miss() { format=null;value=null;count=0; }
}
