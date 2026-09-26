package nl.duplicard.wallet.core;
import java.util.Objects;
public final class Card {
 public static final String DEFAULT_COLOR="#DDE8D5";
 private final String id, name, number, color;
 private final BarcodeType format;
 public Card(String id, String name, String number) {
  this(id,name,number,BarcodeType.EAN_13,DEFAULT_COLOR);
 }
 public Card(String id, String name, String number, BarcodeType format, String color) {
  if (id == null || id.trim().isEmpty() || id.length() > 100) throw new IllegalArgumentException("Invalid card ID.");
  if (name == null || name.trim().isEmpty() || name.trim().length() > 80) throw new IllegalArgumentException("Use a card name between 1 and 80 characters.");
  if (format==null) throw new IllegalArgumentException("Choose a barcode format.");
  String clean = CardRules.normalizeValue(number,format);
  if (!CardRules.isValid(clean,format)) throw new IllegalArgumentException(CardRules.validationMessage(format));
  if (color==null||!color.matches("#[0-9A-Fa-f]{6}")) throw new IllegalArgumentException("Choose a valid card color.");
  this.id=id; this.name=name.trim(); this.number=clean; this.format=format; this.color=color.toUpperCase(java.util.Locale.ROOT);
 }
 public String getId() { return id; }
 public String getName() { return name; }
 public String getNumber() { return number; }
 public BarcodeType getFormat() { return format; }
 public String getColor() { return color; }
 @Override public boolean equals(Object other) {
  if (!(other instanceof Card)) return false;
  Card c=(Card)other;
  return id.equals(c.id) && name.equals(c.name) && number.equals(c.number) && format==c.format && color.equals(c.color);
 }
 @Override public int hashCode() { return Objects.hash(id,name,number,format,color); }
}
