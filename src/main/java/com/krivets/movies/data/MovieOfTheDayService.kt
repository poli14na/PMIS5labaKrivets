package com.krivets.movies.data

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.icu.util.Calendar
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.krivets.movies.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.core.context.GlobalContext

class MovieOfTheDayService : Service() {
	private val movieRepository = GlobalContext.get().get<MovieRepository>()
	private val channelId = "movie_of_the_day_notification_channel"
	private var notificationManager: NotificationManagerCompat? = null
	private val handler = Handler(Looper.getMainLooper())
	private var isServiceRunning = false

	override fun onCreate() {
		super.onCreate()
		val channel = NotificationChannelCompat.Builder(
			channelId,
			NotificationManagerCompat.IMPORTANCE_HIGH
		).setName("Фильм этого дня!").build()

		notificationManager = NotificationManagerCompat.from(this)
		notificationManager!!.createNotificationChannel(channel)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (!isServiceRunning) {
			isServiceRunning = true
			scheduleDailyNotification()
		}

		return START_STICKY
	}

	private fun scheduleDailyNotification() {
		val alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
		val alarmIntent = Intent(this, DailyNotificationReceiver::class.java).let { intent ->
			PendingIntent.getBroadcast(
				this,
				0,
				intent,
				PendingIntent.FLAG_IMMUTABLE
			)
		}

		val calendar = Calendar.getInstance()
		calendar.timeInMillis = System.currentTimeMillis()
		calendar.set(Calendar.HOUR_OF_DAY, 12)

		// если уже 12 часов - отправить уведомление сразу же и настроить на
		// следующий день
		if (System.currentTimeMillis() > calendar.timeInMillis) {
			sendMovieOfTheDay()

			calendar.add(Calendar.DAY_OF_YEAR, 1)
		}

		alarmManager.setRepeating(
			AlarmManager.RTC_WAKEUP,
			calendar.timeInMillis,
			AlarmManager.INTERVAL_DAY,
			alarmIntent
		)
	}

	@SuppressLint("MissingPermission")
	private fun sendMovieOfTheDay() {
		CoroutineScope(Dispatchers.Default).launch {
			val movieOfTheDay = movieRepository.getRandomTitle()

			movieOfTheDay.onSuccess {
				val notificationId = it.id.hashCode()

				handler.post {
					val notification = NotificationCompat.Builder(applicationContext, channelId)
						.setContentTitle("Фильм этого дня!")
						.setContentText("Зацени ${it.titleText.text}")
						.setSmallIcon(R.drawable.ic_launcher_foreground)
						.build()

					notificationManager?.notify(notificationId, notification)
				}
			}
		}
	}

	override fun onBind(intent: Intent?): IBinder? = null
}

class DailyNotificationReceiver : BroadcastReceiver() {
	override fun onReceive(context: Context?, intent: Intent?) {
		if (intent?.action == "android.intent.action.BOOT_COMPLETED") {
			val serviceIntent = Intent(context, MovieOfTheDayService::class.java)
			context?.startService(serviceIntent)
		} else {
			val serviceIntent = Intent(context, MovieOfTheDayService::class.java)
			context?.startService(serviceIntent)
		}
	}
}
