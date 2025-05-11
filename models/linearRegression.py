import numpy as np
from sklearn.linear_model import LinearRegression
from scipy.stats import rankdata

gap = 1e-4

def rankByFunction(x, weight, k):
    score = np.dot(x, weight)
    ranking = []
    for i in range(0, k):
        rank = 1
        for j in range(0, score.size):
            if i != j:
                if score[j] - score[i] >= gap / 2:
                    rank += 1
        ranking.append(rank)
    return ranking

def linear_regression(x, num_tuples, k):
    y = num_tuples + 1 - np.arange(1,num_tuples + 1)
    reg = LinearRegression(positive=True).fit(x, y)
    weight = reg.coef_ / reg.coef_.sum()

    return weight, rankByFunction(x, weight, k)