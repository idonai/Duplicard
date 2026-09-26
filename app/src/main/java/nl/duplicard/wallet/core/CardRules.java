package nl.duplicard.wallet.core;
import java.util.*;
public final class CardRules {
 public static final int MAX_CARDS=2000;
 private CardRules() {}
 public static String normalizeNumber(String input) {
  if (input==null) return "";
  StringBuilder result=new StringBuilder();
  for (int i=0;i<input.length();i++) { char c=input.charAt(i); if(!Character.isWhitespace(c)&&!Character.isSpaceChar(c)) result.append(c); }
  return result.toString();
 }
 public static String normalizeValue(String input,BarcodeType format) {
  if(input==null) return "";
  switch(format) {
   case EAN_13: case EAN_8: case UPC_A: case UPC_E: case ITF: return normalizeNumber(input);
   default: return input.trim();
  }
 }
 public static boolean isValidEan13(String number) {
  return validGtin(number,13);
 }
 private static boolean validGtin(String number,int length) {
  if(number==null||number.length()!=length) return false;
  int sum=0;
  for(int i=0;i<length;i++) { char c=number.charAt(i); if(c<'0'||c>'9') return false; sum+=(c-'0')*(((length-i)%2==0)?3:1); }
  return sum%10==0;
 }
 public static boolean isValid(String value,BarcodeType format) {
  if(value==null) return false;
  switch(format) {
   case EAN_13: return validGtin(value,13);
   case EAN_8: return validGtin(value,8);
   case UPC_A: return validGtin(value,12);
   case UPC_E: return value.matches("[01][0-9]{6}")||(value.matches("[01][0-9]{7}")&&validGtin(expandUpcE(value),12));
   case CODE_128: return value.length()>=1&&value.length()<=80&&value.chars().allMatch(c->c>=32&&c<=126);
   case CODE_39: return value.length()>=1&&value.length()<=80&&value.matches("[0-9A-Z. $/+%-]+");
   case ITF: return value.length()>=2&&value.length()<=80&&value.length()%2==0&&value.matches("[0-9]+");
   case QR_CODE: return value.length()>=1&&value.length()<=512;
   default: return false;
  }
 }
 private static String expandUpcE(String value) {
  String s=value.substring(0,7), number=s.substring(1,7), result;
  char last=number.charAt(5);
  if(last=='0'||last=='1'||last=='2') result=""+s.charAt(0)+number.substring(0,2)+last+"0000"+number.substring(2,5);
  else if(last=='3') result=""+s.charAt(0)+number.substring(0,3)+"00000"+number.substring(3,5);
  else if(last=='4') result=""+s.charAt(0)+number.substring(0,4)+"00000"+number.charAt(4);
  else result=""+s.charAt(0)+number.substring(0,5)+"0000"+last;
  return result+value.charAt(7);
 }
 public static BarcodeType detect(String input) {
  if(input==null||input.trim().isEmpty()) return BarcodeType.EAN_13;
  String numeric=normalizeNumber(input);
  if(validGtin(numeric,13)) return BarcodeType.EAN_13;
  if(validGtin(numeric,12)) return BarcodeType.UPC_A;
  if(validGtin(numeric,8)) return BarcodeType.EAN_8;
  if(numeric.matches("[01][0-9]{6}")) return BarcodeType.UPC_E;
  String value=input==null?"":input.trim();
  if(isValid(value,BarcodeType.CODE_128)) return BarcodeType.CODE_128;
  return BarcodeType.QR_CODE;
 }
 public static String validationMessage(BarcodeType format) {
  switch(format) {
   case EAN_13: return "Enter 13 digits with a valid EAN-13 check digit.";
   case EAN_8: return "Enter 8 digits with a valid EAN-8 check digit.";
   case UPC_A: return "Enter 12 digits with a valid UPC-A check digit.";
   case UPC_E: return "Enter a valid 7- or 8-digit UPC-E code.";
   case CODE_128: return "Code 128 accepts 1–80 standard text characters.";
   case CODE_39: return "Code 39 accepts A–Z, digits, spaces and . - $ / + %.";
   case ITF: return "ITF requires an even number of digits (2–80).";
   case QR_CODE: return "QR content must contain 1–512 characters.";
   default: return "Invalid barcode content.";
  }
 }
 public static List<Card> move(List<Card> cards,int from,int to) {
  if(from<0||to<0||from>=cards.size()||to>=cards.size()) throw new IllegalArgumentException("Invalid card position.");
  List<Card> result=new ArrayList<>(cards); result.add(to,result.remove(from)); return Collections.unmodifiableList(result);
 }
 public static List<Card> merge(List<Card> existing,List<Card> incoming) {
  List<Card> result=new ArrayList<>(existing); Set<String> numbers=new HashSet<>(),ids=new HashSet<>();
  for(Card c:existing) { numbers.add(c.getFormat().name()+"\n"+c.getNumber()); ids.add(c.getId()); }
  for(Card c:incoming) {
   if(!numbers.add(c.getFormat().name()+"\n"+c.getNumber())) continue;
   String id; do { id=UUID.randomUUID().toString(); } while(!ids.add(id));
   result.add(new Card(id,c.getName(),c.getNumber(),c.getFormat(),c.getColor()));
  }
  if(result.size()>MAX_CARDS) throw new IllegalArgumentException("A wallet can contain at most 2,000 cards.");
  return Collections.unmodifiableList(result);
 }
}
