package cu.searchengine.Indexer;

import java.util.ArrayList;
import java.util.List;

public class Posting {
    private int tf; // Term Frequency (TF)
    private Integer [] positions; // Word positions in the document

    public Posting() {
        this.tf = 0;
        this.positions = new Integer[4];
        for(int i=0;i<4;i++)
        {
            positions[i]=0;
        }
    }

    public int getTf() { return tf; }
    public Integer[] getPositions() { return positions; }

    public void addPosition(int pirority) {
        if(pirority==4)
        {
            positions[3]++;
        }
        else if(pirority==3)
        {
            positions[2]++;
        }
        else if(pirority==2)
        {
            positions[1]++;
        }
        else
            positions[0]++;
        this.tf++; // Increase TF count
    }
}
