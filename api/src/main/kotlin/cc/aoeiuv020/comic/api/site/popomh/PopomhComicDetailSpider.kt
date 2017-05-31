package cc.aoeiuv020.comic.api.site.popomh

import cc.aoeiuv020.comic.api.site.ComicDetailSpider
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

class PopomhComicDetailSpider(private val popomh: Popomh, comicDetailUrl: String,
                              override val name: String) : ComicDetailSpider() {
    val comicDetail: Document

    init {
        logger.debug("get comic page $name")
        val conn = Jsoup.connect(comicDetailUrl)
        logger.debug("connect $comicDetailUrl")
        comicDetail = conn.get()
        logger.debug("title: ${comicDetail.title()}")
    }

    override val info: String by lazy {
        logger.debug("get comic info $name")
        val elements = comicDetail.select("#about_kit > ul > li")
        elements.map { it.text() }.joinToString("\n")
    }
    override val imgUrl: String by lazy {
        logger.debug("get comic image $name")
        val elements = comicDetail.select("#about_style > img")
        elements.attr("src")
    }
    override val contents: List<PopomhComicContentsSpider> by lazy {
        logger.debug("get comic image $name")
        val elements = comicDetail.select("#permalink > div.cVolList > ul > li > a")
        elements.map { PopomhComicContentsSpider(popomh, it) }
    }
}