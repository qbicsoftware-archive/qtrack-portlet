"""
    Used to generate some users and their steps data and insert it in the database.
"""


import numpy as np
from scipy.stats import truncnorm
from math import sqrt
import timeit
from pymongo import MongoClient
import pymongo.errors
import uuid
import bson
from scipy import stats

def generate_data_for_activity_type(activity_type, n_days_to_generate):
    """
    generates random data for the selected activity type using scipy truncnorm
    :param activity_type: [1, 10]
        activity_type corresponds to those intervals:
        steps interval    : activity type
            (1000, 2000]  : 1
            (2000, 3000]  : 2
            (3000, 4000]  : 3
            (4000, 5000]  : 4
            (5000, 6000]  : 5
            (6000, 7000]  : 6
            (7000, 8000]  : 7
            (8000, 9000]  : 8
            (9000, 10000] : 9
            (10000, 11000]: 10
    :param n_days_to_generate: number of days we want to generate data for
    :return: [n_days_to_generate] list of ints containing the generated steps data
    """

    # create a truncated normal distribution based on the activity type
    lower, upper = 0, np.infty
    mu, sigma = activity_type*1000, 20000
    x = truncnorm((lower - mu) / sigma, (upper - mu) / sigma, loc=mu, scale=sigma)

    # draw n_days_to_generate samples from it; take the abs, since we only need positive values
    generated_samples = abs(x.rvs(n_days_to_generate))

    # rescale the values according to the mean (necessary since we take the abs value and our sigma is way higher than
    # our mu)
    generated_samples /= np.mean(generated_samples) / (mu + 500)

    return generated_samples.astype(int)

def generate_user_steps_data(n_days_to_sample=365, gender_specific_data_selected=False):
    """
    generates steps data for a user based on data of the paper https://www.nature.com/articles/nature23018
    :param n_days_to_sample: number of days to generate data for
    :param gender_specific_data_selected: whether data based on the gender should be created or not
    :return: [n_days_to_sample] array of ints containing the generated steps data
    """

    # if we want to distinguish between gender
    if gender_specific_data_selected:
        # probability for male and female based on
        # https://github.com/timalthoff/activityinequality/blob/master/data/obesity_by_steps_gender_20170508.csv
        prob_male = 0.534820431     # 158985 / 297268
        prob_female = 0.465179569   # 138283 / 297268

        # select randomly a gender
        random_gender = np.random.choice(["male", "female"], p=[prob_male, prob_female])

        # males have a different distribution than females
        if random_gender == "male":
            # select randomly the activity type (1: ~1500 steps/day, .., 10: >10000 steps/day ); probabilities based on
            # https://github.com/timalthoff/activityinequality/blob/master/data/obesity_by_steps_gender_20170508.csv
            activity_type = np.random.choice(np.arange(1,11), p=[0.042626663, 0.093549706, 0.145636381, 0.173374847,
                                                                 0.164801711, 0.135553669, 0.101978174, 0.070144982,
                                                                 0.044771519, 0.027562349])
        else:
            # select randomly the activity type (1: ~1500 steps/day, .., 10: >10000 steps/day ); probabilities based on
            # https://github.com/timalthoff/activityinequality/blob/master/data/obesity_by_steps_gender_20170508.csv
            activity_type = np.random.choice(np.arange(1, 11), p=[0.105132229, 0.176095399, 0.196307572, 0.172479625,
                                                                  0.129871351, 0.091978045, 0.059197443, 0.036302365,
                                                                  0.020306184, 0.012329787])

    else:
        # select randomly the activity type (1: ~1500 steps/day, .., 10: >10000 steps/day ); probabilities based on
        # https://github.com/timalthoff/activityinequality/blob/master/data/obesity_by_steps_gender_20170508.csv
        activity_type = np.random.choice(np.arange(1,11), p=[0.071702975, 0.131948276, 0.169207584, 0.172958408,
                                                             0.148552821, 0.115283179, 0.082077452, 0.054402088,
                                                             0.033390745, 0.020476472])

    # generate data for the activity type
    user_data = generate_data_for_activity_type(activity_type, n_days_to_generate=n_days_to_sample)

    return user_data

def generate_unique_user_ids():
    """
    generates unique user id
    :return: string: unique user id
    """
    return str(uuid.uuid4())

def generate_user_info(user_id, i):
    """
    generates the user information (email, name, picture, etc) for the list of user ids
    :param user_id: unique user id
    :param i: counter; used as name
    :return: list of dictionaries containing the user information
    """
    return {"_id": user_id, "email": str(i) + "@gmail.com", "family_name": str(i), "given_name": str(i),
            "locale": "de", "name": str(i), "verified_email": True,
            "picture": "https://lh3.googleusercontent.com/-XdUIqdMkCWA/AAAAAAAAAAI/AAAAAAAAAAA/4252rscbv5M/photo.jpg"}

def generate_users(n_users=10000, n_days_to_sample=365):
    """
    generates n_users with unique id's and each one having n_days_to_sample days of steps data
    :param n_users: number of users we want to generate
    :param n_days_to_sample: number of days we want to sample
    :return: [n_users, n_days_to_sample] list containing the steps data of the user, [n_users] list containing the
    unique user ids
    """
    # create unique user id's
    user_ids = [generate_unique_user_ids() for i in range(n_users)]

    # generate user data
    user_steps_data = [generate_user_steps_data(n_days_to_sample=n_days_to_sample) for i in range(n_users)]

    # generate user data (email, name, etc.)
    user_info = [generate_user_info(user_id, i) for i, user_id in enumerate(user_ids)]

    return user_steps_data, user_ids, user_info

def generate_data_for_q_track(n_users=100000, n_days_to_sample=365, starting_date_in_utc=1481500800000):
    """
    generates random data for n_users users for n_days_to_sample different days
    :param n_users: number of users to generate data for
    :param n_days_to_sample: number of days of data each user has
    :param starting_date_in_utc: starting date in utc where we want to start generating data
    :return: dictionary {"dates_in_utc": [n_days_to_sample],        # numpy array holding the dates in UTC
            "user_data": [n_users, n_days_to_sample],               # numpy array holding the steps data
            "user_ids": [n_users]}:                                 # numpy array holding the unique user ids
    """
    print("generating samples.. \n")

    # number of milliseconds of one day
    one_day_in_milliseconds = 60*60*24*1000

    # list holding all the dates
    dates_in_utc = [starting_date_in_utc+one_day_in_milliseconds*i for i in range(n_days_to_sample)]

    # generate the user data and the user id
    user_data, user_id, user_info = generate_users(n_users=n_users, n_days_to_sample=n_days_to_sample)

    # combine the three lists into one dictionary
    generated_data = {"dates_in_utc": np.array(dates_in_utc), "user_data": np.array(user_data),
                      "user_ids": np.array(user_id), "user_info": np.array(user_info)}

    return generated_data

def calculate_mean_and_sem(steps_data):

    sum0 = len(steps_data)
    sum1 = sum(steps_data)
    sum2 = sum([step**2 for step in steps_data])

    # calculate the mean and the stdErrorOfMean(SEM) double
    mean = sum1 / sum0
    std_error_of_mean = 0
    if sum0 >= 2:  # we need at least two datapoints
        std_dev = sqrt((sum0 * sum2 - sum1 * sum1) / (sum0 * (sum0 - 1)))
        std_error_of_mean = std_dev / sqrt(sum0)

    return mean, std_error_of_mean, {"sum0": sum0, "sum1": sum1, "sum2": sum2}


def store_data_in_db(data_to_store):
    """
    stores the data in a mongo DB
    :param data_to_store: data we want to store (dictionary of numpy arrays)
    :return:
    """

    try:
        client = MongoClient()
    except pymongo.errors.ServerSelectionTimeoutError:
        print('Error: Please assure that there is a instance of MongoDB running on the computer.')
        raise

    print "Setting up database"

    # get the database
    db = client['generated_data']

    # create the collections
    users_collection = db['users']
    steps_collection = db['steps']
    days_collection = db['days']

    # reset collections
    db['users'].delete_many({})
    db['steps'].delete_many({})
    db['days'].delete_many({})
	
    # index steps collection
    steps_collection.create_index("startDateInUTC")

    # number of users we want to store
    n_users_to_store = len(data_to_store["user_ids"])


    """
    Insert the users in the database
    """

    start = timeit.default_timer()

    # get the user info data (email, name, id, profile picture, etc)
    user_info_data = data_to_store["user_info"]

    # insert the users
    print "Preparing users.."
    users_to_insert = [user_info_data[i] for i in range(n_users_to_store)]
    print "Inserting users.."
    users_collection.insert_many(users_to_insert)
    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s"

    """
    Insert the steps data in the database
    """

    # TODO: increase speed
    # https://stackoverflow.com/questions/5292370/fast-or-bulk-upsert-in-pymongo

    steps_data_to_insert = []

    print "Preparing steps data.."
    start = timeit.default_timer()

    # iterate over the users
    for user_i in range(n_users_to_store):
        # get the user id
        user_id = data_to_store["user_ids"][user_i]
        # iterate over the dates
        for i, current_date in enumerate(data_to_store["dates_in_utc"]):
            # add them to the list of documents we want to insert
            steps_data_to_insert.append({"user": user_id, "startDateInUTC": bson.Int64(current_date),
                                   "steps": data_to_store["user_data"][user_i][i]})

    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s"

    print "Inserting steps data.."
    start = timeit.default_timer()

    # insert them
    steps_collection.insert_many(steps_data_to_insert)
    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s"

    """
    Insert days data
    """

    print "\n testing find"
    start = timeit.default_timer()
    for i, current_date in enumerate(data_to_store["dates_in_utc"]):
        steps_collection.find({"startDateInUTC": bson.Int64(current_date)})
    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s \n"


    print "Preparing days data.."
    start = timeit.default_timer()

    days_data_to_insert = []
    for i, current_date in enumerate(data_to_store["dates_in_utc"]):
        steps_data_for_current_date = [doc[u"steps"] for doc in
                                       steps_collection.find({"startDateInUTC": bson.Int64(current_date)})]
        mean, std_error_of_mean, sums_for_mean_and_sem  = calculate_mean_and_sem(steps_data_for_current_date)
        days_data_to_insert.append({"dateInUTC": bson.Int64(current_date), "mean": mean,
                                    "stdErrorOfMean": std_error_of_mean, "sumsForMeanAndSEM": sums_for_mean_and_sem})
    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s"


    print "Inserting days data.."
    start = timeit.default_timer()
    days_collection.insert_many(days_data_to_insert)
    stop = timeit.default_timer()
    print "Done in " + str(stop - start) + "s"

    return



def main():

    start = timeit.default_timer()

    # generate some data for q track using the provided usernames
    generated_data = generate_data_for_q_track(n_users=10000, n_days_to_sample=400)

    # store the generated data in the database
    store_data_in_db(generated_data)

    stop = timeit.default_timer()

    print stop - start


if __name__ == "__main__":
    main()
