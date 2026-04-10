package com.example.demo.model.util;

import java.util.ArrayList;

public class MaxPriorityQueue<T extends Comparable<T>> {

    private final ArrayList<T> heap;

    public MaxPriorityQueue()
    {
        this.heap = new ArrayList<>();
    }
    public int compare(T a, T b) { return a.compareTo(b); }
    // פונקציות עזר לחישוב אינדקסים
    private int getParentIndex(int i) { return (i - 1) / 2; }
    private int getLeftChildIndex(int i) { return 2 * i + 1; }
    private int getRightChildIndex(int i) { return 2 * i + 2; }

    /**
     * הוספת איבר לתור בסיבוכיות O(log n)
     */
    public void add(T item)
    {
        heap.add(item); // הוספה לסוף המערך
        heapifyUp(heap.size() - 1); // "בעבוע" למעלה כדי לשמור על תכונת הערימה
    }

    /**
     * שליפת האיבר המקסימלי (השורש) בסיבוכיות O(log n)
     */
    public T poll()
    {
        if (heap.isEmpty())
            return null;

        T maxItem = heap.getFirst();
        T lastItem = heap.removeLast(); // הסרת האיבר האחרון

        if (!heap.isEmpty())
        {
            heap.set(0, lastItem); // הצבת האיבר האחרון בשורש
            heapifyDown(0); // "שקיעה" למטה כדי לשמור על תכונת הערימה
        }
        return maxItem;
    }

    public T peek()
    {
        return heap.isEmpty() ? null : heap.getFirst();
    }

    public boolean isEmpty()
    {
        return heap.isEmpty();
    }

    // תיקון הערימה כלפי מעלה
    private void heapifyUp(int index)
    {
        boolean shouldContinue = true;
        while (index > 0 && shouldContinue)
        {
            int parentIndex = getParentIndex(index);
            // אם הנוכחי גדול מהאבא שלו, נחליף ביניהם
            if (heap.get(index).compareTo(heap.get(parentIndex)) > 0)
            {
                swap(index, parentIndex);
                index = parentIndex;
            }
            else
                shouldContinue = false;
        }
    }

    // תיקון הערימה כלפי מטה
    private void heapifyDown(int index)
    {
        int size = heap.size();

        boolean shouldContinue = true;
        while (shouldContinue)
        {
            int leftIndex = getLeftChildIndex(index);
            int rightIndex = getRightChildIndex(index);
            int largestIndex = index;

            if (leftIndex < size && heap.get(leftIndex).compareTo(heap.get(largestIndex)) > 0)
                largestIndex = leftIndex;

            if (rightIndex < size && heap.get(rightIndex).compareTo(heap.get(largestIndex)) > 0)
                largestIndex = rightIndex;

            if (largestIndex == index)
                shouldContinue = false;
            else
            {
                // החלפה עם הילד הגדול ביותר
                swap(index, largestIndex);
                index = largestIndex;
            }
        }
    }

    // פונקציית עזר נוחה להחלפת איברים בתוך המערך
    private void swap(int i, int j) {
        T temp = heap.get(i);
        heap.set(i, heap.get(j));
        heap.set(j, temp);
    }
}