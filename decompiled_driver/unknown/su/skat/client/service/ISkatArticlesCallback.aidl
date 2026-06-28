package su.skat.client.service;

import su.skat.client.model.Article;

interface ISkatArticlesCallback {
    void onArticlesListChanged(in List<Article> articles);
    void onArticlesChanged(in Article article);
}
