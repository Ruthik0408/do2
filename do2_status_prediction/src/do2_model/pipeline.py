from __future__ import annotations

import argparse

from do2_model.extract import run_extraction
from do2_model.train import run_training


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the complete DO2 modeling pipeline.")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--chunk-size", type=int, default=100000)
    parser.add_argument("--max-train-rows", type=int, default=0)
    parser.add_argument("--iterations", type=int, default=1500)
    parser.add_argument("--depth", type=int, default=8)
    parser.add_argument("--learning-rate", type=float, default=0.05)
    parser.add_argument("--task-type", choices=["CPU", "GPU"], default="CPU")
    parser.add_argument("--thread-count", type=int, default=-1)
    parser.add_argument("--minimum-non-valid-recall", type=float, default=0.80)
    parser.add_argument("--training-mode", choices=["sampled", "full"], default="sampled")
    parser.add_argument(
        "--threshold-policy",
        choices=["max_f1", "max_f2", "recall_80", "precision_50"],
        default="max_f1",
    )
    parser.add_argument("--fixed-threshold", type=float)
    args = parser.parse_args()

    run_extraction(overwrite=args.overwrite, chunk_size=args.chunk_size)
    run_training(
        max_train_rows=args.max_train_rows,
        iterations=args.iterations,
        depth=args.depth,
        learning_rate=args.learning_rate,
        task_type=args.task_type,
        thread_count=args.thread_count,
        minimum_non_valid_recall=args.minimum_non_valid_recall,
        training_mode=args.training_mode,
        threshold_policy=args.threshold_policy,
        fixed_threshold=args.fixed_threshold,
    )


if __name__ == "__main__":
    main()
