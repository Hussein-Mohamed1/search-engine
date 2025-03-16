package Indexer;

import java.util.ArrayList;
import java.util.List;

public class Posting {
    private int tf; // Term Frequency (TF)
    private List<Integer> positions; // Word positions in the document

    public Posting() {
        this.tf = 0;
        this.positions = new ArrayList<>();
    }

    public int getTf() { return tf; }
    public List<Integer> getPositions() { return positions; }

    public void addPosition(int position) {
        this.positions.add(position);
        this.tf++; // Increase TF count
    }
}
