package com.cainsgl.article.repository

import com.cainsgl.article.document.PostDocument
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository
import org.springframework.stereotype.Repository

@Repository
interface PostDocumentRepository : ElasticsearchRepository<PostDocument, Long>