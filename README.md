# Search Engine Project

![Search Engine](https://img.shields.io/badge/Project-Search%20Engine-blue)
![Java](https://img.shields.io/badge/Language-Java-orange)
![Status](https://img.shields.io/badge/Status-In%20Development-yellow)

## 📝 Description

A crawler-based search engine project developed for the Advanced Programming course at Cairo University. This search engine implements core features including web crawling, indexing, ranking, and a user-friendly web interface.

## ✨ Features

- **🕷️ Web Crawler**: Multi-threaded crawler that collects 6000+ HTML documents from the web
- **📑 Indexer**: Fast retrieval and incremental updates of indexed content
- **🔍 Query Processor**: Text processing with stemming capabilities
- **❝❞ Phrase Searching**: Support for exact phrase matching with quotation marks
- **⭐ Ranker**: Document ranking based on relevance and popularity metrics
- **🖥️ Web Interface**: Search results with snippets, pagination, and query suggestions

## 🛠️ Technologies Used

- **Backend**: Java
- **Frontend**: HTML, CSS, JavaScript
- **Database**: MongoDB
- **Build Tool**: Gradle

## 🏗️ System Architecture

### Web Crawler
- Starts with seed URLs and recursively collects documents
- URL normalization and duplicate detection
- Respects robots.txt directives
- Multi-threaded implementation for better performance

### Indexer
- Processes HTML documents to extract relevant content
- Creates an optimized data structure for fast query processing
- Supports incremental index updates

### Query Processor
- Handles user search queries with preprocessing
- Implements word stemming for broader matches

### Ranker
- Relevance calculation using TF-IDF metrics
- Popularity determination via PageRank algorithm
- Combined scoring for optimal results ordering

### Web Interface
- Search input with suggestion mechanism
- Results display with title, URL, and text snippets
- Search timing metrics
- Results pagination (10 results per page)

## 📊 Performance

- Crawls and indexes 6000+ web pages
- Query response time: ~0.5 seconds
- Indexing speed: ~100 pages per minute
- Ranking accuracy comparable to basic search engines

## 👥 Contributors

- [Hussein Mohamed](https://github.com/Hussein-Mohamed1)
- [Tasneem Ahmed](https://github.com/xx-Tasneem-Ahmed-xx)
- [Mohamed Abdelaziem](https://github.com/MohamedAbdelaiem)
- [Youssef Mohamed](https://github.com/username3)

## 📜 License

This project is created for educational purposes at Cairo University's Computer Engineering Department.

## 📚 References

- [PageRank Algorithm](https://en.wikipedia.org/wiki/PageRank)
- [TF-IDF](https://en.wikipedia.org/wiki/Tf%E2%80%93idf)
- [Web Crawling](https://en.wikipedia.org/wiki/Web_crawler)
