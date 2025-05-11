import math
import numpy as np
from scipy.stats import rankdata
from sklearn.linear_model import LinearRegression

def rankByFeature(x, i):
    return np.floor(x.shape[0] + 1 - rankdata(x[:,i]))

def rankByFunction(x, weight):
    return np.floor(x.shape[0] + 1 - rankdata(np.dot(x, weight)))

def ranking_performance(ranking, given_ranking, k, num_tuples):
    return 1 - 2 * np.abs(ranking[:k] - given_ranking[:k]) / num_tuples

def adaRank(x, given_ranking, k, num_tuples, num_attributes, iter = 10):
    weight = np.zeros(num_attributes)
    alpha = np.zeros(iter)

    # Initialize the distribution on tuples
    P = np.ones(k) / k
    for t in range(0, iter):
        # Simply use one feature as the ranker
        # Create ranker t with P on data by selecting the single feature with the best performance
        ranker = 0
        ranker_score = np.sum(P * ranking_performance(rankByFeature(x, 0), given_ranking, k, num_tuples))
        for i in range(1, num_attributes):
            score = np.sum(P * ranking_performance(rankByFeature(x, i), given_ranking, k, num_tuples))
            if score > ranker_score:
                ranker = i
                ranker_score = score
        # Choose alpha which is the weight for this ranker
        E = ranking_performance(rankByFeature(x, ranker), given_ranking, k, num_tuples)
        alpha[t] = math.log(np.sum(P * (1 + E)) / np.sum(P * (1 - E))) / 2
        # Add alpha to the ranking function
        weight[ranker] += alpha[t]
        # Update the distribution
        E = ranking_performance(rankByFunction(x, weight), given_ranking, k, num_tuples)
        P = np.exp(-E) / np.sum(np.exp(-E))

    return weight, rankByFunction(x, weight)