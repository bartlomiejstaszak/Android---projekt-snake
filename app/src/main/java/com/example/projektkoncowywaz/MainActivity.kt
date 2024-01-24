package com.example.projektkoncowywaz

import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.example.projektkoncowywaz.ui.theme.ProjektkoncowywazTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val gra = Gra(lifecycleScope, applicationContext)
        setContent {
            ProjektkoncowywazTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                   Pytonik(gra)
                }
            }
        }
    }
}
var wynik = 0

data class Pozycja(val jedzenie:Pair<Int, Int>, val waz: List<Pair<Int, Int>>) //koordynaty jedzenia i pytona

class Gra(private val scope: CoroutineScope, private val context: Context){

    fun reset() {
        mutableState.value = Pozycja(jedzenie = Pair(5, 5), waz = listOf(Pair(7, 7)))
        ruch = Pair(1, 0)
        wynik = 0
        ZacznijGre()
    }

    private val random = Random
    private val mutex = Mutex()
    private val mutableState = MutableStateFlow(com.example.projektkoncowywaz.Pozycja(jedzenie = Pair(5,5), waz = listOf(Pair(7,7))))

    val pozycja:Flow<com.example.projektkoncowywaz.Pozycja> = mutableState

    var ruch = Pair(1,0)
        set(wartosc){
            scope.launch {
                mutex.withLock {
                    field = wartosc
                }
            }
        }

    private fun ZacznijGre() {
        scope.launch {
            var dlugoscweza = 3
            var koniecgry = false

            while (!koniecgry) {
                delay(250)
                mutableState.update { currentState ->
                    val nowaPozycja = currentState.waz.first().let { pozycja ->
                        mutex.withLock {
                            Pair(
                                (pozycja.first + ruch.first + rozmiar_planszy) % rozmiar_planszy,
                                (pozycja.second + ruch.second + rozmiar_planszy) % rozmiar_planszy
                            )
                        }
                    }

                    if (nowaPozycja.first == (rozmiar_planszy - 1) || nowaPozycja.second == (rozmiar_planszy - 1)) {
                        koniecgry = true
                    }

                    if (nowaPozycja == currentState.jedzenie) {
                        dlugoscweza++
                        wynik++
                    }

                    if (currentState.waz.contains(nowaPozycja)) {
                        koniecgry = true
                        dlugoscweza = 3
                    }

                    currentState.copy(
                        jedzenie = if (nowaPozycja == currentState.jedzenie) Pair(
                            random.nextInt(rozmiar_planszy - 1),
                            random.nextInt(rozmiar_planszy - 1)
                        ) else currentState.jedzenie,
                        waz = listOf(nowaPozycja) + currentState.waz.take(dlugoscweza - 1)
                    )
                }
            }
            if (koniecgry == true){
                Toast.makeText(context, "Przegrałeś, koniec gry!", Toast.LENGTH_LONG).show()
            }
        }
    }
    companion object {
        const val rozmiar_planszy = 16
    }
}

@Composable
fun Pytonik(gra: Gra){
    val pozycja = gra.pozycja.collectAsState(initial = null)

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        pozycja.value?.let {
            Plansza(it)
        }
        Guziki(
            zmianakierunku = { gra.ruch = it },
            reset = { gra.reset() }
        )
    }
}

@Composable
fun Guziki(zmianakierunku: (Pair<Int,Int>) -> Unit, reset: () -> Unit) {
    val guzikiRozmiar = Modifier.size(100.dp)
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(10.dp)) {
        Button(onClick = { zmianakierunku(Pair(0, -1)) }, modifier = guzikiRozmiar) {
            Icon(Icons.Default.KeyboardArrowUp, null)
        }
        Row {
            Button(onClick = { zmianakierunku(Pair(-1, 0)) }, modifier = guzikiRozmiar) {
                Icon(Icons.Default.KeyboardArrowLeft, null)
            }
            Spacer(modifier = guzikiRozmiar)
            Button(onClick = { zmianakierunku(Pair(1, 0)) }, modifier = guzikiRozmiar) {
                Icon(Icons.Default.KeyboardArrowRight, null)
            }
        }
        Button(onClick = { zmianakierunku(Pair(0, 1)) }, modifier = guzikiRozmiar) {
            Icon(Icons.Default.KeyboardArrowDown, null)
        }
        Row {
            Text(text = "Wynik: $wynik", modifier = Modifier.width(250.dp).padding(30.dp))
            Button(onClick = { reset() }
            ){Icon(Icons.Default.Refresh, null)}
        }
    }
}

@Composable
fun Plansza(state: com.example.projektkoncowywaz.Pozycja) {
    BoxWithConstraints(Modifier.padding(16.dp)) {
        val tileSize = maxWidth / Gra.rozmiar_planszy

        Box(
            Modifier
                .size(360.dp)
                .border(2.dp, Color.DarkGray))
        Box (
            Modifier
                .offset(x = tileSize * state.jedzenie.first, y = tileSize * state.jedzenie.second)
                .size(tileSize)
                .background(
                    Color.Red, CircleShape
                )
        )
        state.waz.forEach{
            Box(modifier = Modifier
                .offset(x = tileSize * it.first, y = tileSize * it.second)
                .size(tileSize)
                .background(
                    Color.DarkGray, CircleShape
                )
            )
        }
    }
}

