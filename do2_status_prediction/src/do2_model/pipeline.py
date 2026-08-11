from __future__ import annotations

import argparse

from do2_model.extract import DEFAULT_MATURITY_DAYS, run_extraction
from do2_model.train import run_training


def main() -> None:
    parser = argparse.ArgumentParser(description="Run the complete DO2 modeling pipeline.")
    parser.add_argument("--overwrite", action="store_true")
    parser.add_argument("--chunk-size", type=int, default=100000)
    parser.add_argument("--maturity-days", type=int, default=DEFAULT_MATURITY_DAYS)
    parser.add_argument("--max-train-rows", type=int, default=0)
    parser.add_argument("--iterations", type=int, default=1500)
    parser.add_argument("--depth", type=int, default=6)
    parser.add_argument("--learning-rate", type=float, default=0.02)
    parser.add_argument("--l2-leaf-reg", type=float, default=8)
    parser.add_argument("--min-data-in-leaf", type=int, default=50)
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

    run_extraction(
        overwrite=args.overwrite,
        chunk_size=args.chunk_size,
        maturity_days=args.maturity_days,
    )
    run_training(
        max_train_rows=args.max_train_rows,
        iterations=args.iterations,
        depth=args.depth,
        learning_rate=args.learning_rate,
        l2_leaf_reg=args.l2_leaf_reg,
        min_data_in_leaf=args.min_data_in_leaf,
        task_type=args.task_type,
        thread_count=args.thread_count,
        minimum_non_valid_recall=args.minimum_non_valid_recall,
        training_mode=args.training_mode,
        threshold_policy=args.threshold_policy,
        fixed_threshold=args.fixed_threshold,
    )


if __name__ == "__main__":
    main()
