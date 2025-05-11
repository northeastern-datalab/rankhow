import numpy as np
import time

import linearRegression
import adaRank

def getRankingfromScore(y):
    gap = 1e-4
    given_ranking = []

    for i in range(0, len(y)):
        rank = i + 1
        for j in range(0, i):
            if (y[i - 1 - j] - y[i] < gap / 2):
                rank -= 1
            else:
                break
        given_ranking.append(rank)

    return given_ranking

def ranking_error(ranking, given_ranking, k):
    return np.sum(np.abs(ranking[:k] - given_ranking[:k]))

values = np.genfromtxt("data/per.csv", delimiter=",", skip_header = 2, skip_footer = 1)[:,1:]

x = np.array(values[:,:-1])
y = np.array(values[:,-1])
num_tuples, num_attributes = x.shape
given_ranking = np.array(getRankingfromScore(y))

print("Vary n on NBA data")
k = 5
m = 5
for n in [5000,10000,15000,20000,22840]:
    print ("k: " + str(k) + ", n: " + str(n) + ", m: " + str(m))

    data = x[:n,:m]

    print("Linear Regression")

    weight, ranking = linearRegression.linear_regression(data, n, k)
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()

    print("AdaRank")

    weight, ranking = adaRank.adaRank(data, given_ranking, k, n, m)
    weight /= weight.sum()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()
print("######################################################")
    
print("Vary m on NBA data")
k = 5
n = 22840
for m in [4,5,6,7,8]:
    print ("k: " + str(k) + ", n: " + str(num_tuples) + ", m: " + str(m))

    data = x[:,:m]

    print("Linear Regression")

    weight, ranking = linearRegression.linear_regression(data, n, k)
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()

    print("AdaRank")

    weight, ranking = adaRank.adaRank(data, given_ranking, k, num_tuples, m)
    weight /= weight.sum()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()
print("######################################################")

print("Vary k for opt experiments on NBA data")
m = 5
n = 22840
for k in [2,3,4,5,6]:
    print ("k: " + str(k) + ", n: " + str(num_tuples) + ", m: " + str(m))

    data = x[:,:m]

    print("Linear Regression")

    start = time.perf_counter()
    weight, ranking = linearRegression.linear_regression(data, n, k)
    end = time.perf_counter()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print("Execution time: ", str((end - start) * 1000))
    print()

    print("AdaRank")

    start = time.perf_counter()
    weight, ranking = adaRank.adaRank(data, given_ranking, k, num_tuples, m)
    weight /= weight.sum()
    end = time.perf_counter()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print("Execution time: ", str((end - start) * 1000))
    print()
print("######################################################")

values = np.genfromtxt("data/csrankings.csv", delimiter=",", skip_header = 2, skip_footer = 1)[:,1:]

x = np.array(values[:,:-1])
y = np.array(values[:,-1])
num_tuples, num_attributes = x.shape
given_ranking = np.array(getRankingfromScore(y))

print("Vary n on CSRankings data")
k = 5
m = 5
for n in [100,200,300,400,500,600,628]:
    print ("k: " + str(k) + ", n: " + str(n) + ", m: " + str(m))

    data = x[:n,:m]

    print("Linear Regression")

    weight, ranking = linearRegression.linear_regression(data, n, k)
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()

    print("AdaRank")

    weight, ranking = adaRank.adaRank(data, given_ranking, k, n, m)
    weight /= weight.sum()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()
print("######################################################")
    
print("Vary m on CSRankings data")
k = 5
n = 628
for m in [5,10,15,20,25,27]:
    print ("k: " + str(k) + ", n: " + str(num_tuples) + ", m: " + str(m))

    data = x[:,:m]

    print("Linear Regression")

    weight, ranking = linearRegression.linear_regression(data, n, k)
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()

    print("AdaRank")

    weight, ranking = adaRank.adaRank(data, given_ranking, k, num_tuples, m)
    weight /= weight.sum()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()
print("######################################################")

print("Vary k for opt experiments on CSRankings data")
m = 5
n = 628
for k in [5,10,15,20,25]:
    print ("k: " + str(k) + ", n: " + str(num_tuples) + ", m: " + str(m))

    data = x[:,:m]

    print("Linear Regression")

    weight, ranking = linearRegression.linear_regression(data, n, k)
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()

    print("AdaRank")

    weight, ranking = adaRank.adaRank(data, given_ranking, k, num_tuples, m)
    weight /= weight.sum()
    print("Weight: " + str(weight))
    print("Ranking: " + str(ranking[:k]))
    print("Error: " + str(ranking_error(ranking, given_ranking, k)))
    print()
print("######################################################")