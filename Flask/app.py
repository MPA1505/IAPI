from flask import Flask, jsonify, request
from pymongo import MongoClient

app = Flask(__name__)

# MongoDB connection settings
MONGO_URI = "mongodb://mongouser:securepassword@mongodb-0.mongodb-service.default.svc.cluster.local:27017,mongodb-1.mongodb-service.default.svc.cluster.local:27017,mongodb-2.mongodb-service.default.svc.cluster.local:27017/?replicaSet=rs0"
DATABASE_NAME = "test_db"

# Initialize MongoDB client and database
client = MongoClient(MONGO_URI)
db = client[DATABASE_NAME]

@app.route('/')
def hello():
    return "Hello, Flask the Flask of the Flasks in a flask!"

@app.route('/query', methods=['GET'])
def query_collection():
    """
    Query the MongoDB database and return all documents from the specified collection.
    Example: /query?collection=test_collection
    """
    collection_name = request.args.get('collection', 'test_collection')

    try:
        # Get the specified collection
        collection = db[collection_name]
        
        # Fetch all documents
        results = collection.find()
        
        # Convert MongoDB cursor to list
        results_list = [{key: str(value) for key, value in doc.items()} for doc in results]

        return jsonify(results_list)
    except Exception as e:
        return jsonify({"error": str(e)}), 500

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
