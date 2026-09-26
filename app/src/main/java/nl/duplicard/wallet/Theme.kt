package nl.duplicard.wallet
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
private val Light=lightColorScheme(primary=Color(0xFF245B48),onPrimary=Color.White,primaryContainer=Color(0xFFDDE9C9),onPrimaryContainer=Color(0xFF173C2E),secondary=Color(0xFF58634F),background=Color(0xFFF7F8F2),surface=Color(0xFFF7F8F2),surfaceContainer=Color(0xFFEBEEE5),surfaceVariant=Color(0xFFE2E7DC),onSurface=Color(0xFF1C211C))
private val Dark=darkColorScheme(primary=Color(0xFFA8D4B6),primaryContainer=Color(0xFF285340),background=Color(0xFF121813),surface=Color(0xFF121813),surfaceContainer=Color(0xFF202A22),onSurface=Color(0xFFE0E7DD))
@Composable fun DuplicardTheme(content:@Composable ()->Unit) { MaterialTheme(colorScheme=if(isSystemInDarkTheme()) Dark else Light,content=content) }
