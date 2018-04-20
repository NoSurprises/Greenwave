package utils

import com.google.firebase.database.FirebaseDatabase

class FirebaseDatabaseSingletone {
    companion object {
        @Volatile private var instance: FirebaseDatabase? = null

        fun getFirebaseInstance(): FirebaseDatabase {
            instance ?: synchronized(this) {
                instance = FirebaseDatabase.getInstance()
                instance?.setPersistenceEnabled(true)
            }
            return instance!!
        }
    }
}