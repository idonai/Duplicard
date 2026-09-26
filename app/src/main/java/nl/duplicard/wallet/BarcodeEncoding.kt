package nl.duplicard.wallet
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.MultiFormatWriter
import nl.duplicard.wallet.core.BarcodeType
object BarcodeEncoding {
 fun encode(number:String):BitMatrix=encode(number,BarcodeType.EAN_13)
 fun encode(value:String,type:BarcodeType):BitMatrix=MultiFormatWriter().encode(
  value,
  when(type) {
   BarcodeType.EAN_13 -> BarcodeFormat.EAN_13
   BarcodeType.EAN_8 -> BarcodeFormat.EAN_8
   BarcodeType.UPC_A -> BarcodeFormat.UPC_A
   BarcodeType.UPC_E -> BarcodeFormat.UPC_E
   BarcodeType.CODE_128 -> BarcodeFormat.CODE_128
   BarcodeType.CODE_39 -> BarcodeFormat.CODE_39
   BarcodeType.ITF -> BarcodeFormat.ITF
   BarcodeType.QR_CODE -> BarcodeFormat.QR_CODE
  },0,0,mapOf(EncodeHintType.MARGIN to if(type==BarcodeType.QR_CODE) 4 else 24)
 )
}
