package com.yourrepo.circleftp

import eu.kanade.tachiyomi.source.model.SManga
import eu.kanade.tachiyomi.source.online.HttpSource
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import rx.Observable

class CircleFTPSource : HttpSource() {

    private val client = OkHttpClient()

    override fun fetchPopularManga(page: Int): Observable<List<SManga>> {
        val request = Request.Builder().url("http://circleftp.net/") // Update with CircleFTP base URL
            .build()

        return Observable.create { emitter ->
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body()?.string() ?: "")
                    val mangaList = document.select("div.manga-item")
                        .map {
                            SManga.create().apply {
                                title = it.select("h2.manga-title").text()
                                url = it.select("a").attr("href")
                                thumbnail_url = it.select("img").attr("src")
                            }
                        }
                    emitter.onNext(mangaList)
                    emitter.onCompleted()
                } else {
                    emitter.onError(Throwable("Failed to fetch data"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun popularMangaRequest(page: Int): Request {
        return Request.Builder().url("http://circleftp.net/popular?page=$page")  // Adjust URL
            .build()
    }

    override fun mangaDetailsRequest(manga: SManga): Request {
        return Request.Builder().url("http://circleftp.net${manga.url}")  // Adjust URL
            .build()
    }

    override fun fetchMangaDetails(manga: SManga): Observable<SManga> {
        val request = mangaDetailsRequest(manga)
        return Observable.create { emitter ->
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body()?.string() ?: "")
                    val mangaDetails = manga.apply {
                        description = document.select("div.manga-description").text() // Adjust based on the HTML structure
                    }
                    emitter.onNext(mangaDetails)
                    emitter.onCompleted()
                } else {
                    emitter.onError(Throwable("Failed to fetch manga details"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }

    override fun fetchChapterList(manga: SManga): Observable<List<SChapter>> {
        val request = Request.Builder().url("http://circleftp.net${manga.url}/episodes")  // Adjust URL
            .build()

        return Observable.create { emitter ->
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val document = Jsoup.parse(response.body()?.string() ?: "")
                    val chapterList = document.select("div.episode-item")
                        .map {
                            SChapter.create().apply {
                                name = it.select("h3.episode-title").text()
                                url = it.select("a").attr("href")
                            }
                        }
                    emitter.onNext(chapterList)
                    emitter.onCompleted()
                } else {
                    emitter.onError(Throwable("Failed to fetch chapter list"))
                }
            } catch (e: Exception) {
                emitter.onError(e)
            }
        }
    }
}
