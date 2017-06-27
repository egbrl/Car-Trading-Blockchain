var exec = require('child_process').exec;

module.exports = {
  create: create
};

function create(req, res) {
  exec("peer chaincode invoke -n car -c '{\"Args\":[\"create\", \"amag\", \"garage\", \"{\\\"vin\\\": \\\"YWVW ZZZ 6RZ HY26 0780\\\"}\"]}'", function(error, stdout, stderr) {
    console.log('stdout: ' + stdout);
    console.log('stderr: ' + stderr);
    if (error !== null) {
      console.log('exec error: ' + error);
    }

    res.json(stdout);
  });
}
