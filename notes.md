notes
=====

driver(in scheduler, in framework, in address of the master, optional if the scheduler should handle all the acks)
	calls scheduler.registered?
  periodically calls scheduler.reregistered?

driver.requestResources to get offers
driver.launchTasks
  >>= resourceOffers(driver, offers)
    >>= driver.acceptOffers(offerid, operations, filters)
  <|> driver.offerRescinded  due to dropped message or some other error

driver.declineOffer(offerId, filters) // can be called at any time, decline the offer id and apply fitlters on the resources

driver.reviveOffers, clear out filters so scheduler can get offers from filtered slaves

driver.suppressOffers stop sending offers to the framework, call reviveOffers once you want them again 

driver.reconcileTasks query the master for task status

create driver
	scheduler gets a register callback
	get a resourceOffer
	launch tasks


