package com.mockhub.ai.service;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmbeddingServiceTest {

    @Mock
    private VectorStore vectorStore;

    private EmbeddingService embeddingService;

    @BeforeEach
    void setUp() {
        embeddingService = new EmbeddingService(vectorStore);
    }

    @Test
    @DisplayName("storeDocuments - given documents - adds to vector store")
    void storeDocuments_givenDocuments_addsToVectorStore() {
        List<Document> documents = List.of(new Document("test content"));

        embeddingService.storeDocuments(documents);

        verify(vectorStore).add(documents);
    }

    @Test
    @DisplayName("similaritySearch - given query - returns matching documents")
    void similaritySearch_givenQuery_returnsMatchingDocuments() {
        List<Document> expected = List.of(new Document("matching content"));
        when(vectorStore.similaritySearch(any(SearchRequest.class))).thenReturn(expected);

        List<Document> results = embeddingService.similaritySearch("test query", 5);

        assertNotNull(results);
        assertEquals(1, results.size());
        verify(vectorStore).similaritySearch(any(SearchRequest.class));
    }
}
