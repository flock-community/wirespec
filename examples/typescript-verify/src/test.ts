import { client } from "./gen/Client";
import {Wirespec} from "./gen/Wirespec";
import {PromotionsOverviewDto} from "./gen/model";
import {wirespecSerialization} from '@flock/wirespec/serialization'
import {wirespecFetchIr} from '@flock/wirespec/fetch'

const transportation : Wirespec.Transportation = {
    transport: (req) => wirespecFetchIr(req)
}

export const api = client(wirespecSerialization, transportation);

export const getPromotions = async (query: OverviewQuery): Promise<PromotionsOverviewDto> => {
    const response = await api.getPromotionOverview({
        page: query.page?.page,
        pageSize: query.page?.size,
        search: query.search,
        view: query.view,
    });
    switch (response.status) {
        case 200:
            return response.body;
        case 500:
            throw new Error(EXCEPTION_MESSAGE);
        default:
            assertNever(response);
    }
};