package nl.duplicard.wallet
import nl.duplicard.wallet.core.Card
import nl.duplicard.wallet.core.BarcodeType
import org.junit.Assert.*
import org.junit.Test
class BackupCodecTest {
 private val card=Card("one","Café \"One\"","0000000000000")
 @Test fun roundTripPreservesNamesNumbersAndOrder() {
  val cards=listOf(card,Card("two","Second","4006381333931"))
  assertEquals(cards,BackupCodec.decode(BackupCodec.encode(cards)))
 }
 @Test fun malformedBackupNeverReturnsPartialCards() {
  val valid=BackupCodec.encode(listOf(card)).toString(Charsets.UTF_8)
  rejects(valid.replace("0000000000000","0000000000001"))
  rejects(valid.replace("\"version\": 2","\"version\": 99"))
  rejects(valid.replace("\"0000000000000\"","0")); rejects("[]"); rejects("{broken}"); rejects(valid+"trailing garbage")
 }
 @Test fun boundsInputBeforeParsing() {
  try { BackupCodec.decode(ByteArray(BackupCodec.MAX_BYTES+1));fail("Oversized input accepted") } catch(_:IllegalArgumentException) {}
 }
 @Test fun boundedStreamRejectsOversizedFiles() {
  try { BackupCodec.readBounded(ByteArray(BackupCodec.MAX_BYTES+1).inputStream());fail("Oversized stream accepted") } catch(_:IllegalArgumentException) {}
 }
 @Test fun rejectsMalformedUtf8() {
  try { BackupCodec.decode(byteArrayOf(0xC3.toByte(),0x28));fail("Malformed UTF-8 accepted") } catch(_:IllegalArgumentException) {}
 }
 @Test fun rejectsDuplicateLocalIds() {
  try { BackupCodec.decode(BackupCodec.encode(listOf(card,Card("one","Other","4006381333931"))));fail("Duplicate IDs accepted") } catch(_:IllegalArgumentException) {}
 }
 @Test fun roundTripPreservesFormatsAndColors() {
  val cards=listOf(
   Card("code","Member","ABC-123",BarcodeType.CODE_128,"#336699"),
   Card("qr","Website","https://example.org/member/1",BarcodeType.QR_CODE,"#CC8844")
  )
  assertEquals(cards,BackupCodec.decode(BackupCodec.encode(cards)))
 }
 @Test fun importsLegacyVersionOneWithDefaults() {
  val json="""{"app":"Duplicard","version":1,"cards":[{"id":"old","name":"Old card","number":"4006381333931","format":"EAN_13"}]}"""
  val imported=BackupCodec.decode(json.toByteArray()).single()
  assertEquals(BarcodeType.EAN_13,imported.format)
  assertEquals(Card.DEFAULT_COLOR,imported.color)
 }
 private fun rejects(json:String) { try { BackupCodec.decode(json.toByteArray());fail("Invalid backup accepted") } catch(_:IllegalArgumentException) {} }
}
