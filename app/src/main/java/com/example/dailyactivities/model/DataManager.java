package com.example.dailyactivities.model;

import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static DataManager instance;
    private final List<Task> taskList;

    private DataManager() {
        taskList = buildSampleTasks();
    }

    public static DataManager getInstance() {
        if (instance == null) {
            instance = new DataManager();
        }
        return instance;
    }

    public List<Task> getAllTasks() {
        return taskList;
    }

    public void addTask(Task task) {
        taskList.add(task);
    }

    public List<Task> getUpcomingTasks() {
        List<Task> upcoming = new ArrayList<>();
        for (Task t : taskList) {
            if (t.getStatus() == Task.STATUS_PENDING || t.getStatus() == Task.STATUS_LOCKED) {
                upcoming.add(t);
            }
        }
        return upcoming;
    }

    private List<Task> buildSampleTasks() {
        List<Task> list = new ArrayList<>();

        Task t1 = new Task("🌅", "ফজরের নামাজ",
                "৫:১৫ AM", "১৫ মিনিট",
                Task.CAT_RELIGION, Task.STATUS_DONE, "#FFF8E1");
        list.add(t1);

        Task t2 = new Task("📖", "Quran তেলাওয়াত",
                "৯:৩০ AM", "৩০ মিনিট",
                Task.CAT_RELIGION, Task.STATUS_RUNNING, "#E8F8F0");
        t2.setProgress(55);
        list.add(t2);

        Task t3 = new Task("📚", "পড়াশোনা",
                "১০:০০ AM", "৯০ মিনিট",
                Task.CAT_STUDY, Task.STATUS_PENDING, "#E8F4FF");
        t3.setFocusMode(true);
        list.add(t3);

        Task t4 = new Task("🏃", "ব্যায়াম",
                "১১:৩০ AM", "৩০ মিনিট",
                Task.CAT_EXERCISE, Task.STATUS_PENDING, "#FEF0EF");
        list.add(t4);

        Task t5 = new Task("🍽️", "দুপুরের খাবার",
                "১:০০ PM", "৪৫ মিনিট",
                Task.CAT_FOOD, Task.STATUS_PENDING, "#FFF0EB");
        list.add(t5);

        Task t6 = new Task("💼", "কাজ / প্রজেক্ট",
                "২:০০ PM", "৩ ঘণ্টা",
                Task.CAT_WORK, Task.STATUS_PENDING, "#F0EEFF");
        t6.setFocusMode(true);
        list.add(t6);

        Task t7 = new Task("📱", "Social Media সময়",
                "৭:০০ PM", "৬০ মিনিট",
                Task.CAT_SOCIAL, Task.STATUS_LOCKED, "#FFF0EB");
        list.add(t7);

        return list;
    }
}
