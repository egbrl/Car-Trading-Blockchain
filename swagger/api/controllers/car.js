var exec = require('child_process').exec;

module.exports = {
  create: create
};

function create(req, res) {
  exec("peer chaincode invoke --logging-level=info -n car_cc -c '{\"Args\":[\"create\", \"amag\", \"garage\", \"{\\\"vin\\\": \\\"WVW ZZZ 6RZ HY26 0780\\\"}\"]}'", function(error, stdout, stderr) {
    if (error === null) {
        // somehow also successfull invokes go to stderr..
        return res.json(stderr);
    } else {
        return res.status(400).json(stderr);
    }
  });
}
