import {homes} from "../data/homes";

export async function getHomes() {
  return homes;
}

export async function getHomeById(id) {
  return homes.find((home) => home.id === id);
}