package eu.kanade.tachiyomi.extension.ja.zucchinasulns

import eu.kanade.tachiyomi.network.GET
import eu.kanade.tachiyomi.source.model.*
import eu.kanade.tachiyomi.source.online.ParsedHttpSource
import okhttp3.HttpUrl
import okhttp3.Request
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

class ZucchinasuLNs : ParsedHttpSource() {

    override val name = "ZucchinasuLNs"

    override val baseUrl = "https://foxinflame.com/read"

    override val lang = "ja"

    // Change this to true to use 'latest'
    override val supportsLatest = false

    override fun popularMangaRequest(page: Int): Request =
            GET("$baseUrl/", headers)

    // This shouldn't be called in the first place
    override fun latestUpdatesRequest(page: Int): Request =
            GET("$baseUrl/")

    // Creates an HTTP request with query, and add any Text filters to it
    override fun searchMangaRequest(page: Int, query: String, filters: FilterList): Request {
        val url = HttpUrl.parse("$baseUrl/?")!!.newBuilder().addQueryParameter("query", query)
        (if (filters.isEmpty()) getFilterList() else filters).forEach { filter ->
            when (filter) {
                is TextField -> url.addQueryParameter(filter.key, filter.state)
            }
        }
        return GET(url.toString(), headers)
    }

    override fun popularMangaSelector() = "div.mangaTitle"

    // This shouldn't be called in the first place, but use popularMangaSelector() just in case
    override fun latestUpdatesSelector() = popularMangaSelector()

    override fun searchMangaSelector() = popularMangaSelector()

    override fun popularMangaFromElement(element: Element): SManga {
        val manga = SManga.create()
        element.select("a").first().let {
            manga.setUrlWithoutDomain("/" + it.attr("href"))
            manga.title = it.text()
        }
        return manga
    }

    override fun latestUpdatesFromElement(element: Element): SManga =
            popularMangaFromElement(element)

    override fun searchMangaFromElement(element: Element): SManga = popularMangaFromElement(element)

    override fun popularMangaNextPageSelector() = "a:contains(»)"

    override fun latestUpdatesNextPageSelector() = popularMangaNextPageSelector()

    override fun searchMangaNextPageSelector() = popularMangaNextPageSelector()


    override fun mangaDetailsParse(document: Document): SManga {
        val manga = SManga.create()
        manga.author = document.select("h4.author").first()?.text()
        manga.status = parseStatus(document.select("h4.status").first().text())
        manga.description = document.select("p.description").first()?.text()

        manga.thumbnail_url = document.select("img.thumbnail").first()?.attr("src")
        return manga
    }

    private fun parseStatus(element: String): Int = when {
        element.contains("Completed") -> SManga.COMPLETED
        element.contains("Ongoing") -> SManga.ONGOING
        else -> SManga.UNKNOWN
    }

    override fun chapterListSelector() = " ul li"


    override fun chapterFromElement(element: Element): SChapter {
        val urlElement = element.select("a").first()
        val timeElement = element.select("span.updateDate").first()

        val chapter = SChapter.create()
        chapter.setUrlWithoutDomain("/" + urlElement.attr("folder") + "/" + urlElement.attr("href"))
        chapter.name = urlElement.text()
        chapter.date_upload = parseChapterDate(timeElement.text())
        return chapter
    }

    private fun parseChapterDate(date: String): Long {
        val value = date.split(' ')[0].toInt()
        return value * 1000L
    }


    override fun pageListParse(document: Document): List<Page> {
        val pages = mutableListOf<Page>()
        document.select("page-img").forEach {
            val url = it.attr("src")
            if (url != "") {
                pages.add(Page(pages.size, "", url))
            }
        }
        return pages
    }

    override fun imageUrlParse(document: Document) = ""

    override fun imageRequest(page: Page): Request {
        return GET(page.imageUrl!!)
    }

    private class TextField(name: String, val key: String) : Filter.Text(name)

    override fun getFilterList() = FilterList(
            Filter.Header("Filter"),
            TextField("By Author", "filter_author"),
            Filter.Separator(),
            Filter.Header("Request"),
            TextField("Request a Title", "request_title")
    )
}