package com.nimurai.sudoku

import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NimuraiTheme {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    SudokuNimuraiApp()
                }
            }
        }
    }
}

@Composable
fun NimuraiTheme(content: @Composable () -> Unit) {
    val primary = Color(0xFF8C2F39) // deep red (kimono)
    val secondary = Color(0xFF2E5947) // pine green
    val background = Color(0xFFFAF4EB) // washi paper
    val onBackground = Color(0xFF201A14)
    val outline = Color(0xFFD7C6B3)
    val scheme = lightColorScheme(
        primary = primary,
        secondary = secondary,
        background = background,
        surface = Color(0xFFFCF7F0),
        onSurface = onBackground,
        onBackground = onBackground,
        outline = outline,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}

private data class Puzzle(
    val grid: Array<IntArray>,
    val solution: Array<IntArray>
)

@Composable
fun SudokuNimuraiApp() {
    val context = LocalContext.current
    var puzzle by remember { mutableStateOf(loadRandomPuzzle()) }
    var board by remember { mutableStateOf(copyGrid(puzzle.grid)) }
    val given = remember(puzzle) { computeGiven(puzzle.grid) }
    var selected by remember { mutableStateOf<Pair<Int, Int>?>(null) }
    var showWin by remember { mutableStateOf(false) }
    var mistakes by remember { mutableStateOf(0) }
    var combo by remember { mutableStateOf(0) }
    var shuriken by remember { mutableStateOf(0) }
    var zen by remember { mutableStateOf(false) }
    var musicOn by remember { mutableStateOf(true) }

    // --- Audio ---
    val bgm = remember {
        MediaPlayer.create(context, R.raw.nimurai_bgm).apply {
            isLooping = true
            setVolume(0.35f, 0.35f)
        }
    }
    val soundPool = remember {
        SoundPool.Builder().setMaxStreams(6).build()
    }
    val sfxDigits = remember {
        IntArray(10) { 0 }.apply {
            for (n in 1..9) this[n] = soundPool.load(context, context.resources.getIdentifier("sfx_digit_$n", "raw", context.packageName), 1)
        }
    }
    val sfxError = remember { soundPool.load(context, R.raw.sfx_error, 1) }

    DisposableEffect(musicOn) {
        if (musicOn) bgm.start() else bgm.pause()
        onDispose { }
    }
    DisposableEffect(Unit) {
        onDispose {
            bgm.release()
            soundPool.release()
        }
    }

    fun playDigit(n: Int) {
        if (!musicOn) return
        val id = sfxDigits.getOrNull(n) ?: 0
        if (id != 0) soundPool.play(id, 0.8f, 0.8f, 1, 0, 1f)
    }
    fun playError() {
        if (!musicOn) return
        soundPool.play(sfxError, 0.9f, 0.9f, 1, 0, 1f)
    }

    fun resetGame(newRandom: Boolean = true) {
        puzzle = if (newRandom) loadRandomPuzzle() else puzzle
        board = copyGrid(puzzle.grid)
        selected = null
        mistakes = 0
        combo = 0
        shuriken = 0
        showWin = false
    }

    fun placeNumber(n: Int) {
        val pos = selected ?: return
        val (r, c) = pos
        if (given[r][c]) return
        if (n == 0) {
            board[r][c] = 0
            return
        }
        if (puzzle.solution[r][c] == n) {
            board[r][c] = n
            combo += 1
            if (combo % 5 == 0) shuriken += 1 // earn a shuriken every 5 correct in a row
            playDigit(n)
            if (isSolved(board)) showWin = true
        } else {
            // keep board unchanged, register mistake
            mistakes += 1
            combo = 0
            playError()
        }
    }

    fun useShuriken() {
        if (shuriken <= 0) return
        // auto-fill a correct cell in the same 3x3 box as the selected cell if possible, otherwise random empty
        val target = selected?.let { (sr, sc) ->
            val br = sr/3*3; val bc = sc/3*3
            (br until br+3).flatMap { r -> (bc until bc+3).map { c -> r to c } }
                .firstOrNull { (r, c) -> board[r][c] == 0 }
        } ?: firstEmpty(board)
        if (target != null) {
            val (r, c) = target
            board[r][c] = puzzle.solution[r][c]
            shuriken -= 1
            combo += 1
            if (isSolved(board)) showWin = true
        }
    }

    Box(Modifier.fillMaxSize()) {
        // Background sakura
        SakuraBackground(animate = true, alpha = 0.35f)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(\"Sudoku Nimurai\", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.ExtraBold)
                Spacer(Modifier.weight(1f))
                FilledIconButton(onClick = { zen = !zen }) {
                    Icon(Icons.Default.Pause, contentDescription = \"Zen\") }
                FilledIconButton(onClick = { musicOn = !musicOn }) {
                    Icon(Icons.Default.MusicNote, contentDescription = \"Muzyka\") }
                FilledIconButton(onClick = { resetGame(newRandom = true) }) {
                    Icon(Icons.Default.Refresh, contentDescription = \"Nowa gra\") }
            }

            // Status
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(\"Błędy: $mistakes\", fontWeight = FontWeight.SemiBold)
                Text(\"Kombinacja: $combo\", fontWeight = FontWeight.SemiBold)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    AssistChip(onClick = { useShuriken() }, label = { Text(\"Shuriken: $shuriken\") }, leadingIcon = {
                        Box(Modifier.size(12.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary))
                    })
                    Spacer(Modifier.width(8.dp))
                    AssistChip(onClick = {
                        // classic hint: fill a single empty cell
                        val empty = firstEmpty(board)
                        if (empty != null) {
                            val (r, c) = empty
                            board[r][c] = puzzle.solution[r][c]
                            combo = 0 // hints break combo
                        }
                    }, label = { Text(\"Podpowiedź\") }, leadingIcon = { Icon(Icons.Default.TipsAndUpdates, contentDescription = null) })
                }
            }

            // Board
            SudokuGrid(
                board = board,
                given = given,
                selected = selected,
                onSelect = { selected = it },
                zen = zen
            )

            // Numpad
            Numpad(
                onNumber = { placeNumber(it) },
                onErase = { placeNumber(0) }
            )

            if (zen) {
                ZenBreath()
            }
        }
    }

    if (showWin) {
        AlertDialog(
            onDismissRequest = { showWin = false },
            confirmButton = {
                TextButton(onClick = { showWin = false; resetGame(true) }) { Text(\"Nowa plansza\") }
            },
            title = { Text(\"Gratulacje!\") },
            text = { Text(\"Ukończono Sudoku Nimurai! 🥷🌸\") }
        )
    }
}

@Composable
private fun SudokuGrid(
    board: Array<IntArray>,
    given: Array<BooleanArray>,
    selected: Pair<Int, Int>?,
    onSelect: (Pair<Int, Int>) -> Unit,
    zen: Boolean
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .clip(RoundedCornerShape(20.dp))
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.9f))
            .padding(4.dp)
    ) {
        for (r in 0 until 9) {
            Row(Modifier.weight(1f)) {
                for (c in 0 until 9) {
                    val value = board[r][c]
                    val isGiven = given[r][c]
                    val isSelected = selected?.first == r && selected.second == c
                    val inSameGroup = selected?.let { (sr, sc) -> sr == r || sc == c || (sr/3 == r/3 && sc/3 == c/3) } == true
                    val bg = when {
                        isSelected -> MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        inSameGroup && !zen -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                        else -> MaterialTheme.colorScheme.surface.copy(alpha = 0.6f)
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .border(0.5.dp, MaterialTheme.colorScheme.outline)
                            .background(bg)
                            .clickable { onSelect(r to c) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (value == 0) \"\" else value.toString(),
                            fontSize = 20.sp,
                            fontWeight = if (isGiven) FontWeight.Bold else FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        // thick lines
                        if (c == 2 || c == 5) {
                            Box(Modifier.align(Alignment.CenterEnd).fillMaxHeight().width(2.dp).background(MaterialTheme.colorScheme.outline))
                        }
                        if (r == 2 || r == 5) {
                            Box(Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.outline))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Numpad(
    onNumber: (Int) -> Unit,
    onErase: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (n in 1..9) {
                ElevatedButton(onClick = { onNumber(n) }, modifier = Modifier.weight(1f)) { Text(n.toString(), fontSize = 18.sp) }
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onErase, modifier = Modifier.weight(1f)) { Text(\"Wyczyść\") }
        }
    }
}

// Sakura falling background
@Composable
fun SakuraBackground(animate: Boolean, alpha: Float) {
    val petals = remember {
        List(40) {
            Petal(
                x = Random.nextFloat(),
                y = Random.nextFloat(),
                size = Random.nextFloat()*0.02f + 0.008f,
                speed = Random.nextFloat()*0.02f + 0.006f,
                drift = (Random.nextFloat()-0.5f)*0.02f
            )
        }.toMutableList()
    }
    val anim = remember { Animatable(0f) }
    LaunchedEffect(animate) {
        if (animate) {
            while (true) {
                anim.animateTo(1f, animationSpec = tween(16000, easing = LinearEasing))
                anim.snapTo(0f)
            }
        }
    }
    Canvas(Modifier.fillMaxSize().alpha(alpha)) {
        petals.forEach { p ->
            var y = p.y + p.speed * (anim.value + 0.001f)
            if (y > 1f) y -= 1f
            val x = (p.x + p.drift * anim.value).mod(1f)
            val px = x * size.width
            val py = y * size.height
            drawPath(
                path = sakuraPetalPath(px, py, size.minDimension * p.size),
                color = Color(0xFFEDA7B2)
            )
        }
    }
}
data class Petal(var x: Float, var y: Float, var size: Float, var speed: Float, var drift: Float)

fun sakuraPetalPath(cx: Float, cy: Float, r: Float): Path {
    val path = Path()
    path.moveTo(cx, cy - r)
    path.quadraticBezierTo(cx + r, cy - r*0.3f, cx, cy + r)
    path.quadraticBezierTo(cx - r, cy - r*0.3f, cx, cy - r)
    return path
}

@Composable
fun ZenBreath() {
    val infinite = rememberInfiniteTransition(label = "breath")
    val scale by infinite.animateFloat(
        initialValue = 0.9f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(animation = tween(3200, easing = LinearEasing), repeatMode = RepeatMode.Reverse),
        label = "breathScale"
    )
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size(80.dp * scale)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        )
        Text(\"Oddychaj\", modifier = Modifier.padding(top = 8.dp))
    }
}

// ==== Sudoku helpers ====

private fun copyGrid(src: Array<IntArray>): Array<IntArray> = Array(9) { r -> src[r].clone() }

private fun computeGiven(grid: Array<IntArray>): Array<BooleanArray> =
    Array(9) { r -> BooleanArray(9) { c -> grid[r][c] != 0 } }

private fun isSolved(board: Array<IntArray>): Boolean {
    for (r in 0 until 9) for (c in 0 until 9) if (board[r][c] == 0) return false
    for (r in 0 until 9) for (c in 0 until 9) {
        val n = board[r][c]
        if (n == 0 || !isMoveValid(board, r, c, n)) return false
    }
    return true
}

private fun firstEmpty(board: Array<IntArray>): Pair<Int, Int>? {
    for (r in 0 until 9) for (c in 0 until 9) if (board[r][c] == 0) return r to c
    return null
}

private fun isMoveValid(board: Array<IntArray>, row: Int, col: Int, num: Int): Boolean {
    for (i in 0 until 9) {
        if (i != col && board[row][i] == num) return false
        if (i != row && board[i][col] == num) return false
    }
    val br = row / 3 * 3
    val bc = col / 3 * 3
    for (r in br until br + 3) for (c in bc until bc + 3) {
        if ((r != row || c != col) && board[r][c] == num) return false
    }
    return true
}

private fun solve(grid: Array<IntArray>): Boolean {
    val empty = firstEmpty(grid) ?: return true
    val (r, c) = empty
    for (n in 1..9) {
        if (isMoveValid(grid, r, c, n)) {
            grid[r][c] = n
            if (solve(grid)) return true
            grid[r][c] = 0
        }
    }
    return false
}

// Puzzles
private val PUZZLES = listOf(
    // Easy
    "530070000" +
    "600195000" +
    "098000060" +
    "800060003" +
    "400803001" +
    "700020006" +
    "060000280" +
    "000419005" +
    "000080079",
    // Medium
    "000260701" +
    "680070090" +
    "190004500" +
    "820100040" +
    "004602900" +
    "050003028" +
    "009300074" +
    "040050036" +
    "703018000",
    // Hard
    "005300000" +
    "800000020" +
    "070010500" +
    "400005300" +
    "010070006" +
    "003200080" +
    "060500009" +
    "004000030" +
    "000009700"
)

private fun loadRandomPuzzle(): Puzzle {
    val raw = PUZZLES[Random.nextInt(PUZZLES.size)]
    val grid = Array(9) { IntArray(9) }
    for (i in 0 until 81) {
        val r = i / 9
        val c = i % 9
        grid[r][c] = raw[i].digitToInt()
    }
    val solution = copyGrid(grid)
    solve(solution)
    return Puzzle(grid, solution)
}