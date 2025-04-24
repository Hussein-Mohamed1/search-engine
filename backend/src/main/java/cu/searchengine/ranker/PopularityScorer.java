package cu.searchengine.ranker;

import cu.searchengine.model.RankedDocument;
import cu.searchengine.service.DocumentService;

import java.util.*;

public class PopularityScorer {
    private static final double DAMPING = 0.85;
    private static final double EPSILON = 0.0001;
    private static final int MAX_ITERATIONS = 100;
    private final DocumentService documentService;

    public PopularityScorer(DocumentService documentService) {

        this.documentService = documentService;
    }

    public Map<Integer, RankedDocument> calculatePopularityScores(Map<Integer, RankedDocument> documents) {
        // build the web graph from the documents
        Map<Integer, Set<Integer>> webGraph = documentService.getWebGraph();

        Map<Integer, Double> pageRankScores = calculatePageRank(webGraph);

        for (Map.Entry<Integer, Double> entry : pageRankScores.entrySet()) {
            Integer docId = entry.getKey();
            Double pageRankScore = entry.getValue();

            if (documents.containsKey(docId)) {
                // Update the document's popularity score with its PageRank
                documents.get(docId).setPopularityScore(pageRankScore);
            }
        }

        return documents;
    }

//    private Map<Integer, List<Integer>> buildWebGraph(Map<Integer, RankedDocument> documents) {
//        Map<Integer, List<Integer>> webGraph = new HashMap<>();
//        webGraph = documentService.getWebGraph();
//
//        // Add any document that has no outgoing links as an empty list
//        for (Integer docId : documents.keySet()) {
//            if (!webGraph.containsKey(docId)) {
//                webGraph.put(docId, new ArrayList<>());
//            }
//        }
//
//        return webGraph;
//    }

    private Map<Integer, Double> calculatePageRank(Map<Integer, Set<Integer>> links) {
        // Get all unique pages
        Set<Integer> allPages = new HashSet<>();
        for (Integer page : links.keySet()) {
            allPages.add(page);
            allPages.addAll(links.get(page));
        }

        int n = allPages.size();

        Map<Integer, Double> currPageRank = new HashMap<>();
        Map<Integer, Double> nextPageRank = new HashMap<>();

        // Initialize with equal probability
        double initialRank = 1.0 / n;
        for (Integer page : allPages) {
            currPageRank.put(page, initialRank);
        }

        // Identify pages with no outgoing links (dangling nodes)
        Set<Integer> danglingNodes = new HashSet<>();
        for (Integer page : allPages) {
            if (!links.containsKey(page) || links.get(page).isEmpty()) {
                danglingNodes.add(page);
            }
        }


        // Create reverse Graph
        Map<Integer, List<Integer>> incomingLinks = new HashMap<>();
        for (Integer page : allPages) {
            incomingLinks.put(page, new ArrayList<>());
        }

        for (Map.Entry<Integer, Set<Integer>> entry : links.entrySet()) {
            Integer source = entry.getKey();
            for (Integer target : entry.getValue()) {
                incomingLinks.get(target).add(source);
            }
        }


        boolean converged = false;
        int iterations = 0;

        while (!converged && iterations < MAX_ITERATIONS) {
            // Calculate dangling node contribution
            double danglingWeight = 0;
            for (Integer page : danglingNodes) {
                danglingWeight += currPageRank.get(page);
            }
            danglingWeight = DAMPING * danglingWeight / n;

            double randomJumpProbability = (1 - DAMPING) / n;

            for (Integer page : allPages) {
                double sum = 0;
                for (Integer sourceNode : incomingLinks.get(page)) {
                    // Contribution from incoming links
                    int outDegree = links.get(sourceNode).size();
                    sum += currPageRank.get(sourceNode) / outDegree;
                }

                nextPageRank.put(page, randomJumpProbability + DAMPING * sum + danglingWeight);
            }

            // Check for convergence
            double diff = 0;
            for (Integer page : allPages) {
                diff += Math.abs(nextPageRank.get(page) - currPageRank.get(page));
            }

            if (diff < EPSILON) {
                converged = true;
            }

            // Update PageRank values for next iteration
            for (Integer page : allPages) {
                currPageRank.put(page, nextPageRank.get(page));
            }

            iterations++;
        }

        // Normalize PageRank values
        double sum = 0;
        for (double rank : currPageRank.values()) {
            sum += rank;
        }

        for (Integer page : currPageRank.keySet()) {
            currPageRank.put(page, currPageRank.get(page) / sum);
        }

        return currPageRank;
    }

}