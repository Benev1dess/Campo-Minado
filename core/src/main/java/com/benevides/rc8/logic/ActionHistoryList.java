package com.benevides.rc8.logic;

import com.benevides.rc8.model.RobotAction;

public class ActionHistoryList {

    private static class Node {
        RobotAction action;
        Node next;

        Node(RobotAction action) {
            this.action = action;
            this.next = null;
        }
    }

    private Node head;
    private Node tail;
    private int size;

    public ActionHistoryList() {
        this.head = null;
        this.tail = null;
        this.size = 0;
    }

    // Insere uma nova ação no fim da lista encadeada
    public void addAction(RobotAction action) {
        Node newNode = new Node(action);
        if (head == null) {
            head = newNode;
            tail = newNode;
        } else {
            tail.next = newNode;
            tail = newNode;
        }
        size++;
    }

    // Remove e retorna a primeira ação (útil para reproduzir fila de passos)
    public RobotAction pollFirst() {
        if (head == null) {
            return null;
        }
        RobotAction removed = head.action;
        head = head.next;
        if (head == null) {
            tail = null;
        }
        size--;
        return removed;
    }

    public boolean isEmpty() {
        return size == 0;
    }

    public int size() {
        return size;
    }

    public void clear() {
        head = null;
        tail = null;
        size = 0;
    }
}
