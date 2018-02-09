"""
    Only used to create the two distribution plots for the generated data. (generated_data_distribution.png and
    generated_data_distribution_log_scale.png)
"""

from DataGeneration import generate_user_steps_data
import matplotlib.pyplot as plt
import numpy as np
from scipy.stats import truncnorm

# def test():
#
#     n_male_users_per_data_bin = [6777, 14873, 23154, 27564, 26201, 21551, 16213, 11152, 7118, 4382]
#     n_female_users_per_data_bin = [14538, 24351, 27146, 23851, 17959, 12719, 8186, 5020, 2808, 1705]
#     data_bins = [1000*i for i in range(1,11)]
#
#     # 14538, 24351, 27146, 23851, 17959, 12719, 81860, 50200, 2808, 1705
#
#     n_male_users = sum(n_male_users_per_data_bin)
#     n_female_users = sum(n_female_users_per_data_bin)
#     n_total_users = n_male_users + n_female_users
#
#     print n_male_users, "male users"
#     print n_female_users, "female users"
#     print n_total_users, "total users"
#     print n_male_users/float(n_total_users)*100, "% male of total users"
#     print n_female_users/float(n_total_users)*100, "% female of total users \n"
#
#     print sum([n_male_users_per_data_bin[i]*(data_bins[i]+500) for i in range(len(data_bins))])/n_male_users, \
#         "avg steps male "
#     print sum([n_female_users_per_data_bin[i]*(data_bins[i]+500) for i in range(len(data_bins))])/n_female_users, \
#         "avg steps female"
#     print sum([n_female_users_per_data_bin[i]*(data_bins[i]+500)+n_male_users_per_data_bin[i]*(data_bins[i]+500)
#                for i in range(len(data_bins))])/n_total_users, \
#         "avg steps all"
#
#     """
#     Calculated in excel:
#         5395.596873	avg steps/male user
#         555957	# male users
#
#         4146.437304	avg steps/female user
#         474408	# female users
#
#         total average: 4820
#     """
#
#     lower, upper = 0, np.infty
#
#     for i in range(1, 11):
#         print i
#
#     mu, sigma = 1000, 20000
#     X = truncnorm((lower - mu) / sigma, (upper - mu) / sigma, loc=mu, scale=sigma)
#
#     generated_samples = abs(X.rvs(100000))
#     # generated_samples = X.rvs(10000)
#     print np.mean(generated_samples)
#
#     generated_samples /= np.mean(generated_samples) / (mu + 500)
#
#     print type(generated_samples)
#
#     print np.mean(generated_samples)
#
#     fig, ax = plt.subplots(1,1)
#     ax.hist(generated_samples, normed=True)
#     plt.show()
#
#     return
#
#     mu = 3500   # mean in germany
#     sigma = 3500
#
#     s = np.random.normal(mu, sigma, 100000)
#     count, bins, ignored = plt.hist(s, 100, normed=True, align='mid')
#
#     plt.plot(bins, 1 / (sigma * np.sqrt(2 * np.pi)) * np.exp(- (bins - mu) ** 2 / (2 * sigma ** 2)), linewidth = 2,
#              color = 'r')
#     # plt.show()


def main():

    # test()

    # whether we want a log scale or not
    log_scale = True

    # generate some data for q track using the provided usernames
    generated_user_steps = np.array([generate_user_steps_data(n_days_to_sample=100) for i in range(1000)]).flatten()

    print "data generated"

    plt.hist(generated_user_steps, normed=True, bins=100)

    plt.title("Generated steps data")
    plt.xlabel("Steps")
    if log_scale:
        plt.xscale('log')
    plt.show()



if __name__ == "__main__":
    main()


