package cz.nerkub.NerKubTowersOfDestiny.Managers;

public enum GameState {
	WAITING,   // ⏳ Čekání na hráče
	COUNTDOWN, // Odpočítávání startu hry
	RUNNING,   // 🎮 Hra probíhá
	ENDING,	   // ⏹️ Hra se ukončuje
	RESETING   // Aréna se restartuje (obnovují se blocky)
}
