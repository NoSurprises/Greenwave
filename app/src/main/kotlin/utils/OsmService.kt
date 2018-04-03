package utils

import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface OsmService {

    @GET("/api/interpreter")
    fun fetchChunkData(@Query("data") query: String): io.reactivex.Observable<OsmQueryResult>

    companion object {
        fun create(): OsmService {
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("http://overpass-api.de")
                    .build()

            return retrofit.create(OsmService::class.java)
        }
    }
}
