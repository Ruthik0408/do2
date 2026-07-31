from setuptools import find_packages, setup


setup(
    name="do2-status-prediction",
    version="0.1.0",
    description="Leakage-safe binary CatBoost pipeline for DO2 status prediction",
    package_dir={"": "src"},
    packages=find_packages(where="src"),
    python_requires=">=3.10",
    install_requires=[
        "catboost>=1.2,<2",
        "numpy>=1.26,<3",
        "pandas>=2.2,<4",
        "pyarrow>=15,<25",
        "python-dotenv>=1,<2",
        "scikit-learn>=1.4,<2",
        "SQLAlchemy>=2,<3",
        "psycopg[binary]>=3.1,<4",
    ],
    extras_require={
        "dev": [
            "pytest>=8,<10",
            "ruff>=0.6,<1",
        ],
    },
)
