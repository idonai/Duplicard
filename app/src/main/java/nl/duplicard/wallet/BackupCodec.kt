package nl.duplicard.wallet
import nl.duplicard.wallet.core.Card
import nl.duplicard.wallet.core.CardRules
import nl.duplicard.wallet.core.BarcodeType
import org.json.JSONArray
import org.json.JSONObject
import org.json.JSONTokener
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
object BackupCodec {
 const val MAX_BYTES=2*1024*1024
 fun encode(cards: List<Card>): ByteArray {
  require(cards.size<=CardRules.MAX_CARDS) { "Too many cards." }
  val entries=JSONArray()
  cards.forEach { entries.put(JSONObject().put("id",it.id).put("name",it.name).put("number",it.number).put("format",it.format.name).put("color",it.color)) }
  return JSONObject().put("app","Duplicard").put("version",2).put("cards",entries).toString(2).toByteArray(Charsets.UTF_8)
 }
 fun readBounded(input: InputStream): ByteArray {
  val output=java.io.ByteArrayOutputStream(); val buffer=ByteArray(8192)
  while(true) { val count=input.read(buffer); if(count<0) break
   require(output.size()+count<=MAX_BYTES) { "Backup is larger than 2 MiB." }; output.write(buffer,0,count)
  }
  return output.toByteArray()
 }
 fun decode(bytes: ByteArray): List<Card> {
  require(bytes.size<=MAX_BYTES) { "Backup is larger than 2 MiB." }
  try {
   val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(bytes)).toString()
   val tokener=JSONTokener(text.removePrefix("\uFEFF"))
   val root=tokener.nextValue() as? JSONObject ?: error("Expected a JSON object.")
   require(tokener.nextClean()=='\u0000') { "Unexpected content after the backup." }
   require(root.get("app")=="Duplicard") { "This is not a Duplicard backup." }
   val version=root.get("version") as? Int ?: throw IllegalArgumentException("Backup version must be a number.")
   require(version==1||version==2) { "Use a Duplicard version 1 or 2 backup." }
   val entries=root.getJSONArray("cards")
   require(entries.length()<=CardRules.MAX_CARDS) { "Backup contains more than 2,000 cards." }
   val ids=mutableSetOf<String>()
   return List(entries.length()) { index ->
    val item=entries.getJSONObject(index)
    fun string(key:String)=item.get(key) as? String ?: throw IllegalArgumentException("Card ${index+1}: $key must be text.")
    val format=try { BarcodeType.valueOf(string("format")) } catch(_:Exception) { throw IllegalArgumentException("Card ${index+1}: unsupported barcode format.") }
    if(version==1) require(format==BarcodeType.EAN_13) { "Card ${index+1}: version 1 only supports EAN-13." }
    val color=if(version==1) Card.DEFAULT_COLOR else string("color")
    Card(string("id"),string("name"),string("number"),format,color).also { require(ids.add(it.id)) { "Backup contains duplicate card IDs." } }
   }
  } catch(e:IllegalArgumentException) { throw e }
  catch(e:Exception) { throw IllegalArgumentException("This file is not a valid Duplicard backup.",e) }
 }
}
