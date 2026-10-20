package com.ficaaiofeedback.ficaaiofeedbacktv.data

import com.ficaaiofeedback.ficaaiofeedbacktv.model.Episode
import com.ficaaiofeedback.ficaaiofeedbacktv.model.Series

interface SeriesRepository {
    suspend fun getSeries(): List<Series>
}

class FixedSeriesRepository : SeriesRepository {
    override suspend fun getSeries(): List<Series> = listOf(
        Series(
            id = 1,
            title = "The Handmaid's Tale",
            portugueseTitle = "O Conto da Aia",
            season = 1,
            posterUrl = "https://m.media-amazon.com/images/M/MV5BMWIxMzk4NmItZmM1YS00ODUyLWFlNjgtZDQ4MzljZTZmZDQ5XkEyXkFqcGc@._V1_FMjpg_UX1080_.jpg",
            description = "Set in a dystopian future, a woman is forced to live as a concubine under a fundamentalist theocratic dictatorship.",
            imdbLink = "https://www.imdb.com/title/tt5834204/",
            streamUrl = "https://www.imdb.com/title/tt5834204/",
            episodes = listOf(
                Episode(id = 101, number = 1, title = "Offred", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"),
                Episode(id = 102, number = 2, title = "Birth Day", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"),
                Episode(id = 103, number = 3, title = "Late", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"),
                Episode(id = 104, number = 4, title = "Nolite Te Bastardes Carborundorum", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"),
                Episode(id = 105, number = 5, title = "Faithful", link = "https://test-streams.mux.dev/test_001/stream.m3u8"),
                Episode(id = 106, number = 6, title = "A Woman's Place", link = "https://test-streams.mux.dev/tos_ismc/main.m3u8"),
                Episode(id = 107, number = 7, title = "The Other Side", link = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                Episode(id = 108, number = 8, title = "Jezebels", link = "https://test-streams.mux.dev/pts_shift/master.m3u8"),
                Episode(id = 109, number = 9, title = "The Bridge", link = "https://storage.googleapis.com/shaka-demo-assets/bbb-dark-truths-hls/hls.m3u8"),
                Episode(id = 110, number = 10, title = "Night", link = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"),
            ),
        ),
        Series(
            id = 2,
            title = "Better Call Saul",
            portugueseTitle = "Better Call Saul",
            season = 1,
            posterUrl = "https://m.media-amazon.com/images/M/MV5BMTAxOTQ0MjUzMzJeQTJeQWpwZ15BbWU4MDY0NTAxNzMx._V1_FMjpg_UY2048_.jpg",
            description = "Albuquerque lawyer Jimmy McGill scrapes for clients and respect, but every win drags him closer to shady deals, dangerous players, and the alter ego Saul Goodman.",
            imdbLink = "https://www.imdb.com/title/tt3032476/",
            streamUrl = "https://www.imdb.com/title/tt3032476/",
            episodes = listOf(
                Episode(id = 201, number = 1, title = "Uno", link = "https://storage.googleapis.com/shaka-demo-assets/angel-one-hls/hls.m3u8"),
                Episode(id = 202, number = 2, title = "Mijo", link = "https://storage.googleapis.com/shaka-demo-assets/bbb-dark-truths-hls/hls.m3u8"),
                Episode(id = 203, number = 3, title = "Nacho", link = "https://test-streams.mux.dev/pts_shift/master.m3u8"),
                Episode(id = 204, number = 4, title = "Hero", link = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"),
                Episode(id = 205, number = 5, title = "Alpine Shepherd Boy", link = "https://test-streams.mux.dev/tos_ismc/main.m3u8"),
                Episode(id = 206, number = 6, title = "Five-O", link = "https://test-streams.mux.dev/test_001/stream.m3u8"),
                Episode(id = 207, number = 7, title = "Bingo", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_ts/master.m3u8"),
                Episode(id = 208, number = 8, title = "RICO", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/img_bipbop_adv_example_fmp4/master.m3u8"),
                Episode(id = 209, number = 9, title = "Pimento", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"),
                Episode(id = 210, number = 10, title = "Marco", link = "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_4x3/bipbop_4x3_variant.m3u8"),
            ),
        ),
    )
}
