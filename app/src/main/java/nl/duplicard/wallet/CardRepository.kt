package nl.duplicard.wallet
import android.content.Context
import android.util.AtomicFile
import nl.duplicard.wallet.core.Card
import java.io.File
/** IO only; callers serialize access. */
class CardRepository(context:Context) {
 private val base=File(context.filesDir,"cards.json")
 private val file=AtomicFile(base)
 fun load():List<Card> {
  if(!base.exists()&&!File(base.path+".bak").exists()) return emptyList()
  return file.openRead().use { BackupCodec.decode(BackupCodec.readBounded(it)) }
 }
 fun save(cards:List<Card>) {
  val bytes=BackupCodec.encode(cards); val stream=file.startWrite()
  try { stream.write(bytes); file.finishWrite(stream) }
  catch(e:Exception) { file.failWrite(stream); throw e }
 }
}
