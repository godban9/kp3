import java.util.Random;
import java.util.Scanner;
import java.util.concurrent.*;

public class task1 {

    public static void main(String[] args) throws InterruptedException, ExecutionException {
        Scanner scanner = new Scanner(System.in);
        Random random = new Random();
        System.out.println("Введiть кiлькiсть рядкiв першої матрицi:");
        int rows1 = scanner.nextInt();
        System.out.println("Введiть кiлькiсть стовпцiв першої матрицi:");
        int cols1 = scanner.nextInt();
        System.out.println("Введiть кiлькiсть рядкiв другої матрицi:");
        int rows2 = scanner.nextInt();
        System.out.println("Введiть кiлькiсть стовпцiв другої матрицi:");
        int cols2 = scanner.nextInt();
//------------------------------------------------------------------------------------------------------------ПЕРЕВІРКА І МАКС МІН ЗНАЧЕННЯ
        if (cols1 != rows2) {
            System.out.println("Матрицi не можна перемножити через несумiснi розмiри!!!!!!");
            return;
        }
        System.out.println("Введiть мiнiмальне значення:");
        double minValue = scanner.nextDouble();
        System.out.println("Введiть максимальне значення:");
        double maxValue = scanner.nextDouble();
//------------------------------------------------------------------------------------------------------------ГЕНЕРАЦІЯ МАТРИЦЬ
        double[][] matrixA = generateMatrix(rows1, cols1, random, minValue, maxValue);
        double[][] matrixB = generateMatrix(rows2, cols2, random, minValue, maxValue);
        double[][] result1 = new double[rows1][cols2];
        double[][] result2 = new double[rows1][cols2];
//------------------------------------------------------------------------------------------------------------ВИВІД МАТРИЦЬ СТВОРЕНИХ
        System.out.println("Перша матриця:");
        System.out.println("========================================================================");
        printMatrix(matrixA);
        System.out.println("========================================================================");
        System.out.println("Друга матриця:");
        System.out.println("========================================================================");
        printMatrix(matrixB);
        System.out.println("========================================================================");
//------------------------------------------------------------------------------------------------------------workstealing
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        long start1 = System.currentTimeMillis();
        forkJoinPool.invoke(new MatrixMultiplyTask(matrixA, matrixB, result1, 0, rows1));
        long end1 = System.currentTimeMillis();
//------------------------------------------------------------------------------------------------------------workdealing
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        long start2 = System.currentTimeMillis();
        workDealingMultiply(matrixA, matrixB, result2, executor);
        long end2 = System.currentTimeMillis();
        executor.shutdown();
//------------------------------------------------------------------------------------------------------------ВИВІД РЕЗУЛЬТАТІВ
        System.out.println("Результат множення матриць (Work stealing):");
        printMatrix(result1);
        System.out.println("Результат множення матриць (Work dealing):");
        printMatrix(result2);

        System.out.println("Work stealing виконано за: " + (end1 - start1) + " мс");
        System.out.println("Work dealing виконано за: " + (end2 - start2) + " мс");
    }
//------------------------------------------------------------------------------------------------------------метод створень матриць
    static double[][] generateMatrix(int rows, int cols, Random random, double minValue, double maxValue) {
        double[][] matrix = new double[rows][cols];
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                matrix[i][j] = minValue + (maxValue - minValue) * random.nextDouble();
            }
        }
        return matrix;
    }
//------------------------------------------------------------------------------------------------------------метод виводу гарненьких матриць
    static void printMatrix(double[][] matrix) {
        for (double[] row : matrix) {
            System.out.printf("|");
            for (double val : row) {
                System.out.printf("%.2f ", val);
                System.out.printf("|");
            }
            System.out.println();
        }
    }
//------------------------------------------------------------------------------------------------------------WORKSTEALING
    static class MatrixMultiplyTask extends RecursiveAction {
        private static final int THRESHOLD = 100;
        double[][] A, B, C;
        int startRow, endRow;

        MatrixMultiplyTask(double[][] A, double[][] B, double[][] C, int startRow, int endRow) {
            this.A = A;
            this.B = B;
            this.C = C;
            this.startRow = startRow;
            this.endRow = endRow;
        }
//------------------------------------------------------------------------------------------------------------безпосередньо множення
        @Override
        protected void compute() {
            if (endRow - startRow <= THRESHOLD) {
                for (int i = startRow; i < endRow; i++) {
                    for (int j = 0; j < B[0].length; j++) {
                        for (int k = 0; k < B.length; k++) {
                            C[i][j] += A[i][k] * B[k][j];
                        }
                    }
                }
            } else {                             //це ділення роботи якщо трешхолду умовного недостатьно
                int mid = (startRow + endRow) / 2;
                MatrixMultiplyTask task1 = new MatrixMultiplyTask(A, B, C, startRow, mid);
                MatrixMultiplyTask task2 = new MatrixMultiplyTask(A, B, C, mid, endRow);
                invokeAll(task1, task2);
            }
        }
    }
//------------------------------------------------------------------------------------------------------------WORKDEALING
    static void workDealingMultiply(double[][] A, double[][] B, double[][] C, ExecutorService executor) throws InterruptedException, ExecutionException {
        int rows = A.length;
        int cols = B[0].length;
        int chunkSize = rows / Runtime.getRuntime().availableProcessors();
        Future<?>[] futures = new Future<?>[Runtime.getRuntime().availableProcessors()];

        for (int i = 0; i < futures.length; i++) {
            int startRow = i * chunkSize;
            int endRow = (i == futures.length - 1) ? rows : startRow + chunkSize;

            futures[i] = executor.submit(() -> {
                for (int r = startRow; r < endRow; r++) {
                    for (int j = 0; j < cols; j++) {
                        for (int k = 0; k < B.length; k++) {
                            C[r][j] += A[r][k] * B[k][j];
                        }
                    }
                }
            });
        }
        for (Future<?> future : futures) {
            future.get();
        }
    }
}
