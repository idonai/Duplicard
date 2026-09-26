package nl.duplicard.wallet.core;
import java.util.*;
public final class CoreTest {
 private static int checks;
 public static void main(String[] args) {
  check(CardRules.normalizeNumber(" 4006 3813\t33931\n").equals("4006381333931"));
  check(CardRules.isValidEan13("4006381333931")); check(CardRules.isValidEan13("5901234123457")); check(CardRules.isValidEan13("0000000000000"));
  check(!CardRules.isValidEan13("4006381333932")); check(!CardRules.isValidEan13("400638133393")); check(!CardRules.isValidEan13("４006381333931"));
  rejects(()->new Card("id","  ","4006381333931")); rejects(()->new Card("id","shop","4006381333932")); rejects(()->new Card("","shop","4006381333931"));
  check(CardRules.isValid("55123457",BarcodeType.EAN_8));
  check(CardRules.isValid("036000291452",BarcodeType.UPC_A));
  check(CardRules.isValid("04252614",BarcodeType.UPC_E));
  check(!CardRules.isValid("2611121",BarcodeType.UPC_E));
  check(CardRules.isValid("ABC-123 / 9",BarcodeType.CODE_39));
  check(CardRules.isValid("member:ivár",BarcodeType.QR_CODE));
  check(CardRules.isValid("123456",BarcodeType.ITF));
  check(!CardRules.isValid("12345",BarcodeType.ITF));
  check(CardRules.detect("55123457")==BarcodeType.EAN_8);
  check(CardRules.detect("036000291452")==BarcodeType.UPC_A);
  check(CardRules.detect("ABC-123")==BarcodeType.CODE_128);
  check(CardRules.detect("2611121")==BarcodeType.CODE_128);
  check(CardRules.detect("")==BarcodeType.EAN_13);
  Card colored=new Card("color","Blue card","ABC-123",BarcodeType.CODE_128,"#336699");
  check(colored.getFormat()==BarcodeType.CODE_128); check(colored.getColor().equals("#336699"));
  rejects(()->new Card("color","Bad","ABC",BarcodeType.CODE_128,"blue"));
  Card a=new Card("a"," First ","4006 3813 33931"),b=new Card("b","Second","5901234123457");
  check(a.getName().equals("First")); check(a.getNumber().equals("4006381333931"));
  check(a.getFormat()==BarcodeType.EAN_13); check(a.getColor().equals(Card.DEFAULT_COLOR));
  List<Card> original=Arrays.asList(a,b),moved=CardRules.move(original,0,1);
  check(moved.get(0).equals(b)&&moved.get(1).equals(a)); check(original.get(0).equals(a)); rejects(()->CardRules.move(original,-1,1));
  Card collision=new Card("a","Third","0000000000000");
  List<Card> merged=CardRules.merge(original,Arrays.asList(new Card("x","Renamed duplicate",a.getNumber()),collision,collision));
  check(merged.size()==3); check(merged.get(0).equals(a)); check(!merged.get(2).getId().equals(a.getId())); check(merged.get(2).getNumber().equals("0000000000000"));
  List<Card> mixed=CardRules.merge(Collections.emptyList(),Arrays.asList(colored));
  check(mixed.get(0).getFormat()==BarcodeType.CODE_128); check(mixed.get(0).getColor().equals("#336699"));
  check(CardRules.merge(original,Collections.emptyList()).equals(original)); check(CardRules.merge(merged,Arrays.asList(collision)).equals(merged)); rejects(()->merged.add(a));
  ScanConsensus scan=new ScanConsensus(3);
  check(scan.observe(BarcodeType.CODE_128,"2611121445105")==1);
  check(scan.observe(BarcodeType.CODE_128,"2611121445108")==1);
  check(scan.observe(BarcodeType.CODE_128,"2611121445105")==1);
  check(scan.observe(BarcodeType.CODE_128,"2611121445105")==2);
  check(scan.observe(BarcodeType.CODE_128,"2611121445105")==3);
  scan.miss(); check(scan.observe(BarcodeType.CODE_128,"2611121445105")==1);
  System.out.println("PASS: "+checks+" core checks");
 }
 private static void check(boolean value) { checks++; if(!value) throw new AssertionError("Check "+checks+" failed"); }
 private static void rejects(Runnable action) { checks++; try { action.run(); } catch(IllegalArgumentException|UnsupportedOperationException expected) { return; } throw new AssertionError("Expected rejection "+checks); }
}
