                 Spring Boot
                     │
                     │ creates
                     ↓
              EmbeddingModel
                     │
                     ↓
             SimpleVectorStore
                     │
          ┌──────────┴──────────┐
          ↓                     ↓
       Memory              JSON file
          │                     │
          │              data/vectorstore.json
          │                     │
          └────── load/save ────┘
                     │
                     ↓
                VectorStore
                     │
                     ↓
               RAG Service