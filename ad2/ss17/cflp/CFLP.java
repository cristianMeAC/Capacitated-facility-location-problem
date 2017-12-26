package ad2.ss17.cflp;

import java.util.Arrays;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;

/**
 * Klasse zum Berechnen der L&ouml;sung mittels Branch-and-Bound.
 * Hier sollen Sie Ihre L&ouml;sung implementieren.
 */
public class CFLP extends AbstractCFLP {

    CFLPInstance instance;

    int[] facilityConstMaterial;
    int[] neededBandWidth;

    // lower Bound
    private int[] distanceHeuristik;

    // actual costs
    int globalCosts;


    public CFLP(CFLPInstance instance) {
        // TODO: Hier ist der richtige Platz fuer Initialisierungen
        this.instance = instance;

        facilityConstMaterial =  new int[instance.getNumFacilities()];

        //the facility Bandwidth, so that it can satisfy the customer needs
        neededBandWidth = new int[instance.getNumFacilities()];

        //this.setSolution(instance.calcObjectiveValue(new int[instance.getNumCustomers()] ), new int[instance.getNumCustomers()] );

        //to each customer, we attribute the nearest facility (this will be our solution)
        int[] guess1 =  new int[instance.getNumCustomers()];

        //int[] cheapestFacilityCosts = new int[instance.getNumFacilities()];

        //calculates Lower Bound (we don't take into consideration the actual costs for building a facility, only the distance costs in best case)
        distanceHeuristik = new int[instance.getNumCustomers()];

        int accumulatedCosts = 0;
        for (int customer = instance.getNumCustomers() - 1; customer >= 0; customer--)
        {
            // minimal facility index
            int minimalDistanceIdx = 0;

            int minimalDistance = Integer.MAX_VALUE;

            for (int facility = 0; facility < instance.getNumFacilities(); facility++)
            {
                //if (cheapestFacilityCosts[facility] < minimalOpeningCosts)
                //{
                //    minimalOpeningCosts = cheapestFacilityCosts[facility];
                //}

                if (instance.distance(facility, customer) < minimalDistance)
                {
                    minimalDistance = instance.distance(facility, customer);
                    minimalDistanceIdx = facility;
                }
            }


            //making a better guess towards our start case (attributing the customers to the closest facility)

            guess1[customer] = minimalDistanceIdx;

            distanceHeuristik[customer] = accumulatedCosts;
            accumulatedCosts += minimalDistance;

        }

        this.setSolution(instance.calcObjectiveValue(guess1), guess1);

    }


    public CFLP(CFLPInstance instance) {
        // TODO: Hier ist der richtige Platz fuer Initialisierungen
        this.instance = instance;

        facilityConstMaterial =  new int[instance.getNumFacilities()];

        //the facility Bandwidth, so that it can satisfy the customer needs
        neededBandWidth = new int[instance.getNumFacilities()];


        //this.setSolution(instance.calcObjectiveValue(new int[instance.getNumCustomers()] ), new int[instance.getNumCustomers()] );


        //to each customer, we attribute the nearest facility (this will be our solution)
        int[] guess1 =  new int[instance.getNumCustomers()];


        //int[] cheapestFacilityCosts = new int[instance.getNumFacilities()];

        //calculates Lower Bound (we don't take into consideration the actual costs for building a facility, only the distance costs in best case)
        distanceHeuristik = new int[instance.getNumCustomers()];

        int accumulatedCosts = 0;
        for (int customer = instance.getNumCustomers() - 1; customer >= 0; customer--)
        {
            // minimal facility index
            int minimalDistanceIdx = 0;

            int minimalDistance = Integer.MAX_VALUE;

            for (int facility = 0; facility < instance.getNumFacilities(); facility++)
            {

                if(instance.distance(facility, customer) < minimalDistance)
                {
                    minimalDistance = instance.distance(facility, customer);
                    minimalDistanceIdx = facility;
                }
            }


             //Making a better guess towards our start case (attributing the customers to the closest facility)


            guess1[customer] = minimalDistanceIdx;


            distanceHeuristik[customer] = accumulatedCosts;
            accumulatedCosts += minimalDistance;


        }

        this.setSolution(instance.calcObjectiveValue(guess1), guess1);

    }


    /**
     * Diese Methode bekommt vom Framework maximal 30 Sekunden Zeit zur
     * Verf&uuml;gung gestellt um eine g&uuml;ltige L&ouml;sung
     * zu finden.
     * <p>
     * <p>
     * F&uuml;gen Sie hier Ihre Implementierung des Branch-and-Bound-Algorithmus
     * ein.
     * </p>
     */

    public void BranchAndBound(int actualCustomer, int facility, int[] currentSolution) {
        // TODO: Diese Methode ist von Ihnen zu implementieren

        //we are in the last node of the tree, so to say, the leaf
        if ( actualCustomer == instance.getNumCustomers() )
        {
            //Main.printDebug(globalCosts);

            //when my globalCosts are better than my best solution so far ( U = U' )
            if ( this.getBestSolution().getUpperBound() > globalCosts /* instance.calcObjectiveValue(currentSolution) */ )
                //Main.printDebug(globalCosts);
                this.setSolution( globalCosts /*instance.calcObjectiveValue(currentSolution)*/, currentSolution);

            return;
        }

        //we allocate facilities to customers - Branching
        currentSolution[actualCustomer] = facility;


        int globalCostsBefore = globalCosts;
        int neededBandWidthOld = neededBandWidth[facility];
        int facilityConstMaterialOld = facilityConstMaterial[facility];


        //only the distance costs so far, without the Facility costs
        globalCosts += instance.distance(facility, actualCustomer) * instance.distanceCosts;

        neededBandWidth[facility] += instance.bandwidthOf(actualCustomer);

        int missingBw = neededBandWidth[facility] - facilityConstMaterial[facility] * instance.maxBandwidthOf(facility);

        if (missingBw > 0)
        {
            //Bi * fi ; making the right decision in choosing the Ausbaustufe for  every facility
            int levelups = (int) Math.ceil((double) missingBw / instance.maxBandwidthOf(facility));

            //making the subtractions for k = 2, for instance, so that we won't have an overlapping of costs
            globalCosts -= factor(facilityConstMaterial[facility], instance.baseOpeningCostsOf(facility));

            facilityConstMaterial[facility] += levelups;

            //costs when Ausbaustufe when k = 3, for instance
            globalCosts += factor(facilityConstMaterial[facility], instance.baseOpeningCostsOf(facility));
        }

        //we stop in developing that branch because we won't find a better solution, than our actual U' (L' > U')
        if( globalCosts + distanceHeuristik[actualCustomer] >= getBestSolution().getUpperBound())
        {
            globalCosts = globalCostsBefore;
            neededBandWidth[facility] = neededBandWidthOld;
            facilityConstMaterial[facility] = facilityConstMaterialOld;

            return;
        }

        for (int i = instance.getNumFacilities()-1; i >= 0; i--) {

            //call branch and bound to create sub nodes
            BranchAndBound(actualCustomer + 1, i, currentSolution);


        }

        //so that we can return to the "old" values, after we go to a different branch (return to the root of every subtree)
        globalCosts = globalCostsBefore;
        neededBandWidth[facility] = neededBandWidthOld;
        facilityConstMaterial[facility] = facilityConstMaterialOld;

    }

    @Override
    public void run() {
        // TODO: Diese Methode ist von Ihnen zu implementieren
        for (int i = 0; i < instance.getNumFacilities(); i++)
            BranchAndBound(0, i, new int[instance.getNumCustomers()] );
    }



    private Hashtable<Integer, Hashtable<Integer, Integer>> factorCache = new Hashtable<Integer, Hashtable<Integer, Integer>>();

    //the same method from CFLPInstance
    private int factor (int level, int baseopeningCosts)
    {
        Hashtable<Integer, Integer> cache = factorCache.get(baseopeningCosts);
        if (cache == null)
        {
            cache = new Hashtable<Integer, Integer>();
            factorCache.put(baseopeningCosts, cache);
        }

        Integer value = cache.get(level);
        if (value != null)
            return value;


        switch (level) {
            case 0:
                value = 0;
                break;
            case 1:
                value =  baseopeningCosts;
                break;
            case 2:
                value =  (int) Math.ceil(1.5 * baseopeningCosts);
                break;
            default:
                value =  Math.addExact(Math.addExact(factor(level - 1, baseopeningCosts), factor(level - 2, baseopeningCosts)), (4 - level) * baseopeningCosts);
                break;
        }

        cache.put(level, value);
        return value;
    }

}
