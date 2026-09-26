package nl.duplicard.wallet
import com.google.zxing.*
import com.google.zxing.common.HybridBinarizer
import org.junit.Assert.assertEquals
import org.junit.Test
import nl.duplicard.wallet.core.BarcodeType
import nl.duplicard.wallet.core.CardRules
class BarcodeEncodingTest {
 @Test fun displayedBarsDecodeToOriginalNumber() {
  for(number in listOf("4006381333931","0000000000000","5901234123457")) {
   val bars=BarcodeEncoding.encode(number);val width=bars.width*3;val height=120
   val pixels=IntArray(width*height) { if(bars[(it%width)/3,0]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt() }
   val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(width,height,pixels))),mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(BarcodeFormat.EAN_13)))
   assertEquals(number,decoded.text); assertEquals(BarcodeFormat.EAN_13,decoded.barcodeFormat)
  }
 }
 @Test fun supportedFormatsEncodeAndDecode() {
  val samples=listOf(
   Triple("55123457",BarcodeType.EAN_8,BarcodeFormat.EAN_8),
   Triple("036000291452",BarcodeType.UPC_A,BarcodeFormat.UPC_A),
   Triple("ABC-123",BarcodeType.CODE_128,BarcodeFormat.CODE_128),
   Triple("ABC-123",BarcodeType.CODE_39,BarcodeFormat.CODE_39),
   Triple("123456",BarcodeType.ITF,BarcodeFormat.ITF),
   Triple("https://example.org/member/1",BarcodeType.QR_CODE,BarcodeFormat.QR_CODE)
  )
  for((value,type,expected) in samples) {
   val matrix=BarcodeEncoding.encode(value,type)
   val scale=3; val pixels=IntArray(matrix.width*scale*matrix.height*scale) { index ->
    val width=matrix.width*scale
    if(matrix[(index%width)/scale,(index/width)/scale]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
   }
   val image=RGBLuminanceSource(matrix.width*scale,matrix.height*scale,pixels)
   val decoded=MultiFormatReader().decode(BinaryBitmap(HybridBinarizer(image)),mapOf(DecodeHintType.POSSIBLE_FORMATS to listOf(expected)))
   assertEquals(value,decoded.text); assertEquals(expected,decoded.barcodeFormat)
  }
 }
 @Test fun intermediateNumericInputUsesAnEncodableFormat() {
  val value="2611121"
  val format=CardRules.detect(value)
  assertEquals(BarcodeType.CODE_128,format)
  BarcodeEncoding.encode(value,format)
 }
}
