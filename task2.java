import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.*;

public class task2 {
    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);
        String currentDirectory = System.getProperty("user.dir");
        System.out.println("Поточна директорiя: " + currentDirectory);

        System.out.println("Введiть лiтеру або слово для пошуку у назвах файлiв:");
        String searchQuery = scanner.nextLine();
        File directory = new File(currentDirectory);
//------------------------------------------------------------------------------------------------------------Work stealing (ForkJoinPool)
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long start1 = System.currentTimeMillis();
        int count1 = forkJoinPool.invoke(new FileSearchTask(directory, searchQuery));
        long end1 = System.currentTimeMillis();
//------------------------------------------------------------------------------------------------------------Work dealing (ExecutorService)
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long start2 = System.currentTimeMillis();
        int count2 = workDealingFileSearch(directory, searchQuery, executor);
        long end2 = System.currentTimeMillis();
        executor.shutdown();

        System.out.println("Work stealing: знайдено " + count1 + " файлiв, час виконання: " + (end1 - start1) + " мс");
        System.out.println("Work dealing: знайдено " + count2 + " файлiв, час виконання: " + (end2 - start2) + " мс");
    }
//------------------------------------------------------------------------------------------------------------WORKSTEALING
    static class FileSearchTask extends RecursiveTask<Integer> {
        private final File directory;
        private final String query;

        FileSearchTask(File directory, String query) {
            this.directory = directory;
            this.query = query.toLowerCase();
        }

        @Override
        protected Integer compute() {
            int count = 0;
            List<FileSearchTask> subtasks = new ArrayList<>();
            File[] files = directory.listFiles();

            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        FileSearchTask subtask = new FileSearchTask(file, query);
                        subtasks.add(subtask);
                    } else if (file.getName().toLowerCase().contains(query)) {
                        count++;
                    }
                }
            }
            invokeAll(subtasks);
            for (FileSearchTask subtask : subtasks) {
                count += subtask.join();
            }

            return count;
        }
    }

//------------------------------------------------------------------------------------------------------------WORKDEALING
    static int workDealingFileSearch(File directory, String query, ExecutorService executor) throws InterruptedException, ExecutionException {
        List<Future<Integer>> futures = new ArrayList<>();
        int count = 0;

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    futures.add(executor.submit(() -> workDealingFileSearch(file, query, executor)));
                } else if (file.getName().toLowerCase().contains(query.toLowerCase())) {
                    count++;
                }
            }
        }
        for (Future<Integer> future : futures) {
            count += future.get();
        }

        return count;
    }
}
